package com.terraforming.ares.services.ai.dl4j;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiConstants;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Value-network inference (win probability) for NETWORK_V2 computers.
 * <p>
 * Backend is chosen by the system property {@code -Dares.nn.backend=...}:
 * <ul>
 *   <li>{@code java} (default) - pure Java CPU inference ({@link PureJavaValueNetwork}); no CUDA / ND4J / DL4J needed.
 *       This is what runs on Heroku.</li>
 *   <li>{@code dl4j} - original DL4J graph (CUDA or CPU). Requires building with {@code -Pdl4j-reference,dl4j-cuda}
 *       (or {@code dl4j-cpu}). Used for A/B strength comparison.</li>
 *   <li>{@code verify} - answers come from the pure Java network, but every batch is also evaluated by DL4J and the
 *       differences are collected (see {@link VerifyStats}). Same build requirements as {@code dl4j}.
 *       Only every {@code -Dares.nn.verify.every} (default 20) call is checked, to keep the slow reference affordable.</li>
 * </ul>
 */
@Service
public class NNService {
    // Перечисление для выбора модели
    public enum ModelType {
        FIRST,
        SECOND
    }

    private static final String FIRST_MODEL = "iter4_epoch_2_93.zip";
    private static final String SECOND_MODEL = "iter4_epoch_2_93.zip";

    // Architecture of the trained graph (see configuration.json in the model zip)
    private static final int DENSE_TABLE = 512;
    private static final int DENSE_HAND = 512;
    private static final int DENSE_HIDDEN = 1024;

    private static final String REFERENCE_CLASS = "com.terraforming.ares.services.ai.dl4j.reference.Dl4jValueNetwork";

    private enum Backend {JAVA, DL4J, VERIFY}

    private final Backend backend;
    private final Map<ModelType, ValueNetwork> primary = new EnumMap<>(ModelType.class);
    private final Map<ModelType, ValueNetwork> reference = new EnumMap<>(ModelType.class);
    private final VerifyStats stats = new VerifyStats();
    // verify mode checks only every Nth predictBatch call: the DL4J reference is much slower (esp. on CPU)
    private final int verifyEvery = Integer.getInteger("ares.nn.verify.every", 20);
    private final AtomicLong verifyCounter = new AtomicLong();
    // inference volume since start (calls of predictBatch and states in them); read by the benchmark
    private final LongAdder totalCalls = new LongAdder();
    private final LongAdder totalStates = new LongAdder();
    private final AtomicLong maxBatch = new AtomicLong();
    private final LongAdder calls1000 = new LongAdder();
    private final LongAdder calls10000 = new LongAdder();
    private final LongAdder states1000 = new LongAdder();

    public NNService() throws Exception {
        backend = Backend.valueOf(System.getProperty("ares.nn.backend", "java").trim().toUpperCase());
        System.out.println("NNService backend: " + backend);

        if (backend != Backend.DL4J) {
            PureJavaValueNetwork first = loadJava(FIRST_MODEL);
            primary.put(ModelType.FIRST, first);
            // the same file twice -> share the instance (immutable, thread-safe)
            primary.put(ModelType.SECOND, FIRST_MODEL.equals(SECOND_MODEL) ? first : loadJava(SECOND_MODEL));
        }
        if (backend != Backend.JAVA) {
            ValueNetwork first = loadReference(FIRST_MODEL);
            reference.put(ModelType.FIRST, first);
            reference.put(ModelType.SECOND, FIRST_MODEL.equals(SECOND_MODEL) ? first : loadReference(SECOND_MODEL));
            if (backend == Backend.DL4J) {
                primary.putAll(reference);
            }
        }
    }

    private static PureJavaValueNetwork loadJava(String fileName) throws IOException {
        return PureJavaValueNetwork.load(new File(fileName),
                AiConstants.TABLE_VECTOR_SIZE, AiConstants.HAND_VECTOR_SIZE,
                DENSE_TABLE, DENSE_HAND, DENSE_HIDDEN);
    }

    private static ValueNetwork loadReference(String fileName) throws Exception {
        try {
            return (ValueNetwork) Class.forName(REFERENCE_CLASS)
                    .getConstructor(File.class, int.class, int.class)
                    .newInstance(new File(fileName), AiConstants.TABLE_VECTOR_SIZE, AiConstants.HAND_VECTOR_SIZE);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("ares.nn.backend=dl4j/verify needs a build with -Pdl4j-reference,dl4j-cuda (or dl4j-cpu)", e);
        }
    }

    public List<Prediction> predictBatch(List<float[]> featuresBatch, ModelType modelType) {
        if (featuresBatch.isEmpty()) return List.of();

        totalCalls.increment();
        totalStates.add(featuresBatch.size());
        maxBatch.accumulateAndGet(featuresBatch.size(), Math::max);
        if (featuresBatch.size() >= 1000) {
            calls1000.increment();
            states1000.add(featuresBatch.size());
            if (featuresBatch.size() >= 10000) {
                calls10000.increment();
            }
        }
        float[][] features = featuresBatch.toArray(new float[0][]);
        float[] probs = primary.get(modelType).predictBatch(features);

        if (backend == Backend.VERIFY && verifyCounter.incrementAndGet() % Math.max(1, verifyEvery) == 0) {
            stats.record(probs, reference.get(modelType).predictBatch(features));
        }

        List<Prediction> result = new ArrayList<>(probs.length);
        for (float p : probs) {
            result.add(new Prediction(p));
        }
        return result;
    }

    public List<Prediction> predictBatch(List<float[]> featuresBatch, Player player) {
        return predictBatch(featuresBatch, player.isFirstBot() ? ModelType.FIRST : ModelType.SECOND);
    }

    /**
     * Replaces the model for both bots (strength sweeps over checkpoints; pure Java backend only).
     * Must not be called while games are being played.
     */
    public synchronized void useModel(File zip) throws IOException {
        if (backend != Backend.JAVA) {
            throw new IllegalStateException("useModel is supported only with ares.nn.backend=java");
        }
        PureJavaValueNetwork network = PureJavaValueNetwork.load(zip,
                AiConstants.TABLE_VECTOR_SIZE, AiConstants.HAND_VECTOR_SIZE, DENSE_TABLE, DENSE_HAND, DENSE_HIDDEN);
        primary.put(ModelType.FIRST, network);
        primary.put(ModelType.SECOND, network);
    }

    /** @return {calls of predictBatch, evaluated states} since the application started */
    public long[] inferenceCounters() {
        return new long[]{totalCalls.sum(), totalStates.sum()};
    }

    /** @return {largest batch, calls with >=1000 states, calls with >=10000 states, states inside calls with >=1000 states} */
    public long[] inferenceTail() {
        return new long[]{maxBatch.get(), calls1000.sum(), calls10000.sum(), states1000.sum()};
    }

    public void printVerifyStats() {
        if (backend == Backend.VERIFY) {
            stats.print("SUMMARY");
        }
    }

    @PreDestroy
    void printStats() {
        if (backend == Backend.VERIFY) {
            stats.print("FINAL");
        }
    }

    /**
     * Compares pure Java output with the DL4J reference. Besides raw probability differences it counts how often the
     * best state of a batch (the AI usually picks the argmax) would differ, which is what actually affects the play.
     */
    private static final class VerifyStats {
        private static final double MATERIAL_GAP = 1e-3;

        private final AtomicLong calls = new AtomicLong();
        private final AtomicLong states = new AtomicLong();
        private final AtomicLong multiStateCalls = new AtomicLong();
        private final AtomicLong argmaxMismatch = new AtomicLong();
        private final AtomicLong argmaxMaterialMismatch = new AtomicLong();
        private final AtomicLong maxDiffBits = new AtomicLong(Double.doubleToLongBits(0));
        private final DoubleAdder sumDiff = new DoubleAdder();

        void record(float[] java, float[] ref) {
            long c = calls.incrementAndGet();
            states.addAndGet(java.length);

            double maxDiff = 0;
            for (int i = 0; i < java.length; i++) {
                double d = Math.abs(java[i] - ref[i]);
                sumDiff.add(d);
                maxDiff = Math.max(maxDiff, d);
            }
            double finalMaxDiff = maxDiff;
            maxDiffBits.accumulateAndGet(Double.doubleToLongBits(maxDiff),
                    (a, b) -> Double.doubleToLongBits(Math.max(Double.longBitsToDouble(a), Double.longBitsToDouble(b))));

            if (java.length > 1) {
                multiStateCalls.incrementAndGet();
                int bestJava = argmax(java), bestRef = argmax(ref);
                if (bestJava != bestRef) {
                    argmaxMismatch.incrementAndGet();
                    // how much better is the DL4J choice than ours, according to DL4J itself
                    if (ref[bestRef] - ref[bestJava] > MATERIAL_GAP) {
                        argmaxMaterialMismatch.incrementAndGet();
                    }
                }
            }
            if (c % 20000 == 0) {
                print("PROGRESS");
            }
        }

        private static int argmax(float[] a) {
            int best = 0;
            for (int i = 1; i < a.length; i++) {
                if (a[i] > a[best]) best = i;
            }
            return best;
        }

        void print(String tag) {
            long s = Math.max(1, states.get());
            System.out.printf("NN-VERIFY %s: calls=%d states=%d maxAbsDiff=%.3e meanAbsDiff=%.3e | batches(>1 state)=%d argmaxMismatch=%d materialMismatch(gap>%.0e)=%d%n",
                    tag, calls.get(), states.get(), Double.longBitsToDouble(maxDiffBits.get()), sumDiff.sum() / s,
                    multiStateCalls.get(), argmaxMismatch.get(), MATERIAL_GAP, argmaxMaterialMismatch.get());
        }
    }
}
