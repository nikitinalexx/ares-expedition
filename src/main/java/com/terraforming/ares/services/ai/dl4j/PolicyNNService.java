package com.terraforming.ares.services.ai.dl4j;

import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.policyai.encoder.PolicyRecordFeatureConverter;
import org.bytedeco.javacpp.SizeTPointer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import static org.bytedeco.cuda.global.cudart.cudaMemGetInfo;

@Service
public class PolicyNNService {
    public static final int BATCH_SIZE = 8192;
    public static final int CHUNK_SIZE = 4096;

    // Таймаут ожидания полного батча перед padding (мс)
    private static final long BACKOFF_MS = 50;

    public enum ModelType {
        FIRST
    }

    private final Map<ModelType, Queue<BatchRequest>> queues = new EnumMap<>(ModelType.class);
    private final Map<ModelType, AtomicReference<ComputationGraph>> networks = new EnumMap<>(ModelType.class);

    public PolicyNNService() throws Exception {
        networks.put(
                ModelType.FIRST,
                new AtomicReference<>(
                        ComputationGraph.load(new File("base_network_1773994730734.zip"), false)
                )
        );

        if (AiConstants.POLICY_PLAYING) {
            for (ModelType type : ModelType.values()) {
                Queue<BatchRequest> queue = new ConcurrentLinkedQueue<>();
                queues.put(type, queue);

                AtomicReference<ComputationGraph> netRef = networks.get(type);

                Thread.ofVirtual()
                        .name("Batcher-" + type)
                        .start(() -> batchLoop(type, queue, netRef));
            }
        }
    }

    public List<PolicyPrediction> predictBatch(List<float[]> featuresBatch, ModelType modelType) {
        if (featuresBatch.isEmpty()) return List.of();

        if (featuresBatch.size() <= CHUNK_SIZE) {
            CompletableFuture<List<PolicyPrediction>> future = new CompletableFuture<>();
            queues.get(modelType).add(new BatchRequest(featuresBatch, future));
            return future.join();
        }

        List<CompletableFuture<List<PolicyPrediction>>> futures = new ArrayList<>();
        for (int i = 0; i < featuresBatch.size(); i += CHUNK_SIZE) {
            List<float[]> chunk = featuresBatch.subList(i, Math.min(i + CHUNK_SIZE, featuresBatch.size()));
            CompletableFuture<List<PolicyPrediction>> future = new CompletableFuture<>();
            queues.get(modelType).add(new BatchRequest(chunk, future));
            futures.add(future);
        }

        List<PolicyPrediction> result = new ArrayList<>(featuresBatch.size());
        for (CompletableFuture<List<PolicyPrediction>> future : futures) {
            result.addAll(future.join());
        }
        return result;
    }

    public void reloadModel(ModelType type, String path) throws IOException {
        ComputationGraph newNet = ComputationGraph.load(new File(path), false);
        AtomicReference<ComputationGraph> ref = networks.get(type);
        ComputationGraph oldNet = ref.getAndSet(newNet);
        if (oldNet != null) oldNet.close();
        System.out.println("Model " + type + " reloaded from " + path);
    }

    private void batchLoop(
            ModelType type,
            Queue<BatchRequest> queue,
            AtomicReference<ComputationGraph> netRef
    ) {
        int tableSize = PolicyRecordFeatureConverter.TABLE_SIZE;
        int handSize = PolicyRecordFeatureConverter.HAND_SIZE;
        int selectableSize = PolicyRecordFeatureConverter.SEL_SIZE;

        // Переиспользуемые массивы — создаём один раз на весь lifetime потока
        float[] tableFlat = new float[BATCH_SIZE * tableSize];
        float[] handFlat = new float[BATCH_SIZE * handSize];
        float[] selectableFlat = new float[BATCH_SIZE * selectableSize];

        long batchCount = 0;
        BatchRequest pending = null; // запрос не влезший в предыдущий батч

        while (!Thread.currentThread().isInterrupted()) {
            List<BatchRequest> batch = new ArrayList<>();

            try {
                // Берём первый элемент — сначала pending если есть
                BatchRequest first;
                if (pending != null) {
                    first = pending;
                    pending = null;
                } else {
                    while ((first = queue.poll()) == null) {
                        if (Thread.currentThread().isInterrupted()) return;
                        Thread.onSpinWait();
                    }
                }

                batch.add(first);
                int currentStatesCount = first.features.size();

                // Набираем до BATCH_SIZE
                long deadline = System.currentTimeMillis() + BACKOFF_MS;
                while (currentStatesCount < BATCH_SIZE) {
                    BatchRequest next = queue.poll();
                    if (next != null) {
                        if (currentStatesCount + next.features.size() > BATCH_SIZE) {
                            // Не влезает — откладываем на следующий батч
                            pending = next;
                            break;
                        }
                        batch.add(next);
                        currentStatesCount += next.features.size();
                    } else {
                        if (System.currentTimeMillis() >= deadline) {
                            break;
                        }
                        Thread.onSpinWait();
                    }
                }

                batchCount++;
                if (batchCount % 5000 == 0) {
                    logGpuMemory();
                }

                processInference(batch, netRef.get(), currentStatesCount,
                        tableFlat, handFlat, selectableFlat,
                        tableSize, handSize, selectableSize);

            } catch (Throwable t) {
                t.printStackTrace();
                for (BatchRequest r : batch) {
                    r.future.completeExceptionally(t);
                }
            }
        }
    }

    private void processInference(
            List<BatchRequest> batch,
            ComputationGraph net,
            int realStates,
            float[] tableFlat,
            float[] handFlat,
            float[] selectableFlat,
            int tableSize,
            int handSize,
            int selectableSize
    ) {
        // Заполняем переиспользуемые массивы реальными данными
        int tablePos = 0, handPos = 0, selectPos = 0;
        for (BatchRequest r : batch) {
            for (float[] fullFeature : r.features) {
                System.arraycopy(fullFeature, 0, tableFlat, tablePos, tableSize);
                tablePos += tableSize;

                System.arraycopy(fullFeature, tableSize, handFlat, handPos, handSize);
                handPos += handSize;

                System.arraycopy(fullFeature, tableSize + handSize, selectableFlat, selectPos, selectableSize);
                selectPos += selectableSize;
            }
        }

        try (INDArray tableInput = Nd4j.create(tableFlat, new int[]{BATCH_SIZE, tableSize}, 'c');
             INDArray handInput = Nd4j.create(handFlat, new int[]{BATCH_SIZE, handSize}, 'c');
             INDArray selectInput = Nd4j.create(selectableFlat, new int[]{BATCH_SIZE, selectableSize}, 'c')) {

            try {
                INDArray[] outputs = net.output(false, tableInput, handInput, selectInput);

                try {
                    float[][] allHeadsData = new float[outputs.length][];
                    for (int h = 0; h < outputs.length; h++) {
                        allHeadsData[h] = outputs[h].data().asFloat();
                    }

                    int offset = 0;
                    for (BatchRequest r : batch) {
                        int size = r.features.size();
                        List<PolicyPrediction> subResult = new ArrayList<>(size);

                        for (int i = 0; i < size; i++) {
                            int currentRow = offset + i;
                            PolicyPrediction p = new PolicyPrediction();
                            p.outcome = allHeadsData[0][currentRow];
                            p.buildArgmax = slice(allHeadsData[1], currentRow, 268);
                            p.sellArgmax = slice(allHeadsData[2], currentRow, 269);
                            p.targetArgmax = slice(allHeadsData[3], currentRow, 90);
                            p.blueActionsArgmax = slice(allHeadsData[4], currentRow, 51);
                            p.corporationArgmax = slice(allHeadsData[5], currentRow, 25);
                            p.smallActionsArgmax = slice(allHeadsData[6], currentRow, 47);
                            p.systemActionsArgmax = slice(allHeadsData[7], currentRow, 7);
                            p.headSelArgmax = slice(allHeadsData[8], currentRow, 10);
                            subResult.add(p);
                        }
                        r.future.complete(subResult);
                        offset += size;
                    }
                } finally {
                    for (INDArray o : outputs) {
                        if (o != null) o.close();
                    }
                }
            } catch (Throwable t) {
                Nd4j.getWorkspaceManager().destroyAllWorkspacesForCurrentThread();
                for (BatchRequest r : batch) {
                    r.future.completeExceptionally(t);
                }
                t.printStackTrace();
            }
        }
    }

    private void logGpuMemory() {
        try {
            SizeTPointer free = new SizeTPointer(1);
            SizeTPointer total = new SizeTPointer(1);
            cudaMemGetInfo(free, total);
            long usedMb = (total.get() - free.get()) / 1024 / 1024;
            long totalMb = total.get() / 1024 / 1024;
            long freeMb = free.get() / 1024 / 1024;
            System.out.printf("GPU: %d/%d MB (free=%d MB)%n", usedMb, totalMb, freeMb);
        } catch (Throwable t) {
            System.out.println("GPU mem log failed: " + t.getMessage());
        }
    }

    private float[] slice(float[] source, int row, int width) {
        float[] dest = new float[width];
        System.arraycopy(source, row * width, dest, 0, width);
        return dest;
    }

    private static class BatchRequest {
        final List<float[]> features;
        final CompletableFuture<List<PolicyPrediction>> future;

        BatchRequest(List<float[]> features, CompletableFuture<List<PolicyPrediction>> future) {
            this.features = features;
            this.future = future;
        }
    }
}