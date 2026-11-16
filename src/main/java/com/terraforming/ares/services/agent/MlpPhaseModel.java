package com.terraforming.ares.services.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/**
 * Pure Java forward pass of a candidate Q-model written by {@code ml/phase_model.py} (format: see that file).
 * The file kind is still {@code phase_mlp}: every decision type uses this shape, with its own observation hash and its
 * own candidate-feature width. The decision type is stored in the file and checked by {@link NeuralPolicy}.
 * <pre>
 *   z = clip((obs - mean) / std)       a = W_obs z + b_obs   (once per decision)
 *   h1_k = relu(a + W_cand c_k)        h2_k = relu(W_mid h1_k + b_mid)       logit_k = w_out . h2_k + b_out
 * </pre>
 * Immutable and thread safe. A model refuses to load when its files are damaged (hash of weights.bin) or inconsistent.
 * <p>
 * <b>Layout and speed.</b> The observation projection is ~195k multiply-adds (760 → 256) and dominates a decision —
 * it costs several times more than all engine work between two decisions. The file keeps the trainer's row-major
 * {@code [out, in]} matrices; they are <b>transposed at load</b> so inference walks a contiguous vector per input:
 * {@code a[j] += W[i][j] * z[i]}. That is the one shape HotSpot's SuperWord vectorises (independent lanes, stride 1,
 * plain {@code float}); a row-major dot product per neuron stays scalar and is ~5× slower. Accumulation in
 * {@code float} adds ~1e-6 of error over 760 terms, far below the 1e-4 agreement with the numpy reference.
 * Whole zero rows are skipped: an observation is largely one-hots and empty tables, and a candidate vector has a
 * handful of non-zero features out of 6–116.
 */
public final class MlpPhaseModel {
    private final DecisionType decisionType;
    private final String schemaHash;
    /** The identity the model depends on (no file padding): null for bundles written before it existed. */
    private final String modelHash;
    private final int obsSize;
    private final int candFeatures;
    private final int h1;
    private final int h2;
    private final float clip;
    private final float[] mean;
    private final float[] std;
    /** {@code [obsSize][h1]}, transposed from the file's {@code [h1][obsSize]}. */
    private final float[] wObsT;
    private final float[] bObs;
    /** {@code z} of a feature that is 0 in the observation: {@code clip((0 - mean) / std)}. */
    private final float[] zAtZero;
    /** {@code b_obs + W_obs z_at_zero}: the whole projection when every feature is 0 (90% of a real observation). */
    private final float[] aBase;
    /** {@code [candFeatures][h1]}, transposed. */
    private final float[] wCandT;
    /** {@code [h1][h2]}, transposed. */
    private final float[] wMidT;
    private final float[] bMid;
    private final float[] wOut;
    private final float bOut;

    private MlpPhaseModel(JsonNode meta, ByteBuffer weights) {
        try {
            this.decisionType = DecisionType.valueOf(meta.path("decisionType").asText());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown decision type in model.json: " + meta.path("decisionType").asText());
        }
        this.schemaHash = meta.get("schemaHash").asText();
        this.modelHash = meta.hasNonNull("modelHash") ? meta.get("modelHash").asText() : null;
        int trainedOn = meta.get("obsSize").asInt();
        this.candFeatures = meta.get("candFeatures").asInt();
        this.h1 = meta.get("hidden").get(0).asInt();
        this.h2 = meta.get("hidden").get(1).asInt();
        this.clip = (float) meta.get("clip").asDouble();
        if (trainedOn != ObservationBuilder.size()) {
            // No compatibility shim: a model belongs to the encoder it was trained on, and the engine refuses the rest.
            // Warm starts across an encoder change are handled where the artifact is produced (ml/train_all.py pads the
            // input columns with zeros), not by silently reshaping weights at load time.
            throw new IllegalArgumentException("The model was trained on a " + trainedOn + "-value observation, the "
                    + "current encoder produces " + ObservationBuilder.size() + ": retrain it");
        }
        this.obsSize = trainedOn;
        this.mean = read(weights, trainedOn);
        this.std = read(weights, trainedOn);
        this.wObsT = transpose(read(weights, h1 * trainedOn), h1, trainedOn);
        this.bObs = read(weights, h1);
        this.wCandT = transpose(read(weights, h1 * candFeatures), h1, candFeatures);
        this.wMidT = transpose(read(weights, h2 * h1), h2, h1);
        this.bMid = read(weights, h2);
        this.wOut = read(weights, h2);
        this.bOut = read(weights, 1)[0];
        if (weights.hasRemaining()) {
            throw new IllegalArgumentException("weights.bin has " + weights.remaining() + " unexpected extra bytes");
        }
        for (float s : std) {
            if (!(s > 0f)) {
                throw new IllegalArgumentException("std must be positive");
            }
        }
        // A NaN weight would otherwise be swallowed by the relu branches and produce a silent garbage move.
        for (float[] block : List.of(mean, std, wObsT, bObs, wCandT, wMidT, bMid, wOut)) {
            for (float w : block) {
                if (!Float.isFinite(w)) {
                    throw new IllegalArgumentException("weights.bin contains " + w + ": the model is damaged");
                }
            }
        }
        if (!Float.isFinite(bOut)) {
            throw new IllegalArgumentException("weights.bin contains " + bOut + ": the model is damaged");
        }
        // The observation is one-hots and empty tables: ~90% of its values are exactly 0. Its projection is linear, so
        // the contribution of every feature that stays 0 is a constant computed once here; inference only adds the
        // difference for the features that are actually present. Exact, not an approximation: with clip() in the path,
        // z_at_zero is written down explicitly instead of relying on (0 - mean) / std.
        this.zAtZero = new float[obsSize];
        this.aBase = Arrays.copyOf(bObs, h1);
        for (int i = 0; i < obsSize; i++) {
            float v = (0f - mean[i]) / std[i];
            float z0 = v < -clip ? -clip : Math.min(v, clip);
            zAtZero[i] = z0;
            if (z0 == 0f) {
                continue;
            }
            int offset = i * h1;
            for (int j = 0; j < h1; j++) {
                aBase[j] += wObsT[offset + j] * z0;
            }
        }
    }

    public static MlpPhaseModel load(Path dir) throws IOException {
        JsonNode meta = new ObjectMapper().readTree(dir.resolve("model.json").toFile());
        if (!"phase_mlp".equals(meta.path("kind").asText()) || meta.path("version").asInt() != 1) {
            throw new IOException("Unsupported model: " + meta.path("kind").asText() + " v" + meta.path("version").asInt());
        }
        byte[] raw = Files.readAllBytes(dir.resolve("weights.bin"));
        try {
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
            if (!sha.equals(meta.get("weightsSha256").asText())) {
                throw new IOException("weights.bin does not match model.json (damaged or half written): " + dir);
            }
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        if (raw.length / 4 != meta.get("weightsCount").asInt() || raw.length % 4 != 0) {
            throw new IOException("weights.bin has an unexpected size: " + dir);
        }
        return new MlpPhaseModel(meta, ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN));
    }

    private static float[] read(ByteBuffer buffer, int count) {
        if (buffer.remaining() < 4L * count) {
            throw new IllegalArgumentException("weights.bin is too short");
        }
        float[] result = new float[count];
        buffer.asFloatBuffer().get(result);
        buffer.position(buffer.position() + 4 * count);
        return result;
    }

    /** {@code [out][in]} (the file order) → {@code [in][out]}, so inference walks contiguous output vectors. */
    private static float[] transpose(float[] source, int out, int in) {
        float[] result = new float[source.length];
        for (int o = 0; o < out; o++) {
            int row = o * in;
            for (int i = 0; i < in; i++) {
                result[i * out + o] = source[row + i];
            }
        }
        return result;
    }

    public DecisionType decisionType() {
        return decisionType;
    }

    public String schemaHash() {
        return schemaHash;
    }

    /** The identity the model depends on, or null for a bundle written before the field existed. */
    public String modelHash() {
        return modelHash;
    }

    public int obsSize() {
        return obsSize;
    }

    public int candFeatures() {
        return candFeatures;
    }

    /**
     * Logit of the win probability of every candidate (sigmoid(logit) = P(win)).
     */
    public float[] logits(float[] obs, List<Candidate> candidates) {
        if (obs.length != obsSize) {
            throw new IllegalArgumentException("Observation has " + obs.length + " values, the model expects " + obsSize);
        }
        // a = W_obs z + b_obs. The bias (aBase) already holds the projection of every feature that is 0, so only the
        // observations that are actually present are walked — ~10% of 760 on real data.
        float[] a = Arrays.copyOf(aBase, h1);
        for (int i = 0; i < obsSize; i++) {
            float raw = obs[i];
            if (raw == 0f) {
                continue;
            }
            float v = (raw - mean[i]) / std[i];
            if (!Float.isFinite(v)) {
                throw new IllegalStateException("Observation " + i + " is " + raw + " (NaN/Inf in the input?)");
            }
            float delta = (v < -clip ? -clip : Math.min(v, clip)) - zAtZero[i];
            if (delta == 0f) {
                continue;
            }
            int offset = i * h1;
            for (int j = 0; j < h1; j++) {
                a[j] += wObsT[offset + j] * delta;
            }
        }
        float[] result = new float[candidates.size()];
        float[] hidden1 = new float[h1];
        float[] hidden2 = new float[h2];
        for (int k = 0; k < result.length; k++) {
            float[] c = candidates.get(k).features();
            if (c.length != candFeatures) {
                throw new IllegalArgumentException("Candidate has " + c.length + " features, the model expects " + candFeatures);
            }
            System.arraycopy(a, 0, hidden1, 0, h1);
            for (int f = 0; f < candFeatures; f++) {
                float cf = c[f];
                if (!Float.isFinite(cf)) {
                    throw new IllegalStateException("Candidate feature " + f + " is " + cf + " (NaN/Inf in the input?)");
                }
                if (cf == 0f) {
                    continue;
                }
                int offset = f * h1;
                for (int j = 0; j < h1; j++) {
                    hidden1[j] += wCandT[offset + j] * cf;
                }
            }
            Arrays.fill(hidden2, 0f);
            for (int i = 0; i < h1; i++) {
                float hi = hidden1[i];
                if (hi <= 0f) {
                    continue; // relu
                }
                int offset = i * h2;
                for (int j = 0; j < h2; j++) {
                    hidden2[j] += wMidT[offset + j] * hi;
                }
            }
            float logit = bOut;
            for (int j = 0; j < h2; j++) {
                float pre = hidden2[j] + bMid[j];
                if (pre > 0f) {
                    logit += wOut[j] * pre;
                }
            }
            if (!Float.isFinite(logit)) {
                throw new IllegalStateException("The model produced " + logit + " (NaN/Inf in the input?)");
            }
            result[k] = logit;
        }
        return result;
    }
}
