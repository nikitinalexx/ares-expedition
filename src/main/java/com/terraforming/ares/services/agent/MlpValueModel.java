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
 * Pure Java forward pass of the state value network written by {@code ml/train_value.py} (kind {@code value_mlp}).
 * <pre>
 *   z = clip((obs - mean) / std)     h1 = relu(W1 z + b1)     h2 = relu(W2 h1 + b2)     logit = w3 . h2 + b3
 *   V(s) = sigmoid(logit) = probability that the player the observation describes wins the game
 * </pre>
 * The observation is the same {@link ObservationBuilder} vector the policy sees, so V can score <b>any</b> state: it is a
 * function of the state alone, not of a decision. That is what makes it usable as the leaf evaluation of search, and it is
 * why the encoder must contain everything that matters for the outcome (incomes, both tables, tags, resources, the hand):
 * a coarse feature set cannot answer "is building this card now worth +5% or -5%".
 * <p>
 * Same numerics and the same speed tricks as {@link MlpPhaseModel}: matrices are transposed at load, the projection of a
 * zero feature is folded into a base vector (a real observation is ~90% zeros), zeros are skipped. Immutable, thread
 * safe. {@code reference.json} (written by the trainer next to the weights) is checked at load: an export that disagrees
 * with PyTorch is refused instead of silently playing garbage.
 */
public final class MlpValueModel {
    private final String observationHash;
    private final int obsSize;
    private final int h1;
    private final int h2;
    private final float clip;
    private final float[] mean;
    private final float[] std;
    /** {@code [obsSize][h1]}, transposed from the file's {@code [h1][obsSize]}. */
    private final float[] w1T;
    private final float[] b1;
    /** {@code clip((0 - mean) / std)} per feature. */
    private final float[] zAtZero;
    /** {@code b1 + W1 z_at_zero}: the projection of an all-zero observation. */
    private final float[] aBase;
    /** {@code [h1][h2]}, transposed. */
    private final float[] w2T;
    private final float[] b2;
    private final float[] w3;
    private final float b3;

    private MlpValueModel(JsonNode meta, ByteBuffer weights) {
        this.observationHash = meta.get("observationHash").asText();
        int trainedOn = meta.get("obsSize").asInt();
        this.h1 = meta.get("hidden").get(0).asInt();
        this.h2 = meta.get("hidden").get(1).asInt();
        this.clip = (float) meta.get("clip").asDouble();
        if (trainedOn != ObservationBuilder.size()) {
            // Same rule as the policy: a value model belongs to the encoder it was trained on. Warm starts across an
            // encoder change are the trainer's job (zero-padded input columns), not a silent reshape at load time.
            throw new IllegalArgumentException("The value model was trained on a " + trainedOn
                    + "-value observation, the current encoder produces " + ObservationBuilder.size() + ": retrain it");
        }
        this.obsSize = trainedOn;
        this.mean = read(weights, trainedOn);
        this.std = read(weights, trainedOn);
        this.w1T = transpose(read(weights, h1 * trainedOn), h1, trainedOn);
        this.b1 = read(weights, h1);
        this.w2T = transpose(read(weights, h2 * h1), h2, h1);
        this.b2 = read(weights, h2);
        this.w3 = read(weights, h2);
        this.b3 = read(weights, 1)[0];
        if (weights.hasRemaining()) {
            throw new IllegalArgumentException("weights.bin has " + weights.remaining() + " unexpected extra bytes");
        }
        for (float s : std) {
            if (!(s > 0f)) {
                throw new IllegalArgumentException("std must be positive");
            }
        }
        for (float[] block : List.of(mean, std, w1T, b1, w2T, b2, w3)) {
            for (float w : block) {
                if (!Float.isFinite(w)) {
                    throw new IllegalArgumentException("weights.bin contains " + w + ": the model is damaged");
                }
            }
        }
        if (!Float.isFinite(b3)) {
            throw new IllegalArgumentException("weights.bin contains " + b3 + ": the model is damaged");
        }
        this.zAtZero = new float[obsSize];
        this.aBase = Arrays.copyOf(b1, h1);
        for (int i = 0; i < obsSize; i++) {
            float v = (0f - mean[i]) / std[i];
            float z0 = v < -clip ? -clip : Math.min(v, clip);
            zAtZero[i] = z0;
            if (z0 == 0f) {
                continue;
            }
            int offset = i * h1;
            for (int j = 0; j < h1; j++) {
                aBase[j] += w1T[offset + j] * z0;
            }
        }
    }

    public static MlpValueModel load(Path dir) throws IOException {
        JsonNode meta = new ObjectMapper().readTree(dir.resolve("model.json").toFile());
        if (!"value_mlp".equals(meta.path("kind").asText()) || meta.path("version").asInt() != 1) {
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
        MlpValueModel model = new MlpValueModel(meta, ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN));
        Path reference = dir.resolve("reference.json");
        if (Files.exists(reference)) {
            model.checkReference(reference);
        }
        return model;
    }

    /** Refuses an export that does not reproduce the trainer's own forward pass. */
    private void checkReference(Path reference) throws IOException {
        JsonNode json = new ObjectMapper().readTree(reference.toFile());
        JsonNode inputs = json.path("inputs");
        JsonNode logits = json.path("logits");
        double worst = 0;
        for (int i = 0; i < Math.min(inputs.size(), logits.size()); i++) {
            float[] obs = new float[inputs.get(i).size()];
            for (int j = 0; j < obs.length; j++) {
                obs[j] = (float) inputs.get(i).get(j).asDouble();
            }
            if (obs.length != obsSize) {
                throw new IOException(reference + ": reference observation has " + obs.length + " values, expected " + obsSize);
            }
            worst = Math.max(worst, Math.abs(logit(obs) - logits.get(i).asDouble()));
        }
        // The reference is produced by the trainer's own forward pass on the GPU and, in probability space, converted to a
        // logit; near a saturated prediction that transform amplifies the last bits of float32 into ~1e-3. The check exists
        // to refuse a structurally wrong export (a misordered layout moves logits by order 1), so the tolerance is tight
        // but not tighter than the arithmetic: 5e-3 of logit is 0.1% of probability at worst.
        if (worst > 5e-3) {
            throw new IOException("the exported value model disagrees with the trainer by " + worst + ": " + reference);
        }
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

    /** {@code [out][in]} (the file order) -> {@code [in][out]}, so inference walks contiguous output vectors. */
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

    public String observationHash() {
        return observationHash;
    }

    public int obsSize() {
        return obsSize;
    }

    /** Logit of the win probability of the state described by {@code obs}. */
    public float logit(float[] obs) {
        if (obs.length != obsSize) {
            throw new IllegalArgumentException("Observation has " + obs.length + " values, the model expects " + obsSize);
        }
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
                a[j] += w1T[offset + j] * delta;
            }
        }
        float[] hidden1 = new float[h1];
        for (int i = 0; i < h1; i++) {
            float pre = a[i];
            hidden1[i] = pre > 0f ? pre : 0f;
        }
        float[] hidden2 = new float[h2];
        float logit = b3;
        for (int i = 0; i < h1; i++) {
            float hi = hidden1[i];
            if (hi <= 0f) {
                continue;
            }
            int offset = i * h2;
            for (int j = 0; j < h2; j++) {
                hidden2[j] += w2T[offset + j] * hi;
            }
        }
        for (int j = 0; j < h2; j++) {
            float pre = hidden2[j] + b2[j];
            if (pre > 0f) {
                logit += w3[j] * pre;
            }
        }
        if (!Float.isFinite(logit)) {
            throw new IllegalStateException("The model produced " + logit + " (NaN/Inf in the input?)");
        }
        return logit;
    }

    /** P(win) of the state, i.e. sigmoid of {@link #logit}. */
    /**
     * V of several observations in ONE pass over the weights.
     * <p>
     * The forward pass is memory bound: the weight block is 1090 x 512 floats (2.2 MB) and a single-candidate call reads it
     * once, so scoring 6 candidates read it 6 times - 13 MB per critic per decision, and with an ensemble of six critics
     * ~80 MB per decision. That is why the pipeline never loaded the machine (owner, 2026-11-16). Here every feature row of
     * the weights is read once and applied to all candidates, which divides the memory traffic by the batch size; the
     * arithmetic is identical.
     */
    public float[] batchLogits(float[][] observations, int count) {
        float[][] acc = new float[count][h1];
        for (int k = 0; k < count; k++) {
            System.arraycopy(aBase, 0, acc[k], 0, h1);
        }
        float[] deltas = new float[count];
        for (int i = 0; i < obsSize; i++) {
            boolean any = false;
            for (int k = 0; k < count; k++) {
                float raw = observations[k][i];
                if (raw == 0f) {
                    deltas[k] = 0f;
                    continue;
                }
                float v = (raw - mean[i]) / std[i];
                float clipped = v < -clip ? -clip : Math.min(v, clip);
                float delta = clipped - zAtZero[i];
                deltas[k] = delta;
                any |= delta != 0f;
            }
            if (!any) {
                continue;
            }
            int offset = i * h1;
            for (int j = 0; j < h1; j++) {
                float w = w1T[offset + j];
                for (int k = 0; k < count; k++) {
                    float delta = deltas[k];
                    if (delta != 0f) {
                        acc[k][j] += w * delta;
                    }
                }
            }
        }
        float[] result = new float[count];
        float[] hidden1 = new float[h1];
        float[] hidden2 = new float[h2];
        for (int k = 0; k < count; k++) {
            for (int j = 0; j < h1; j++) {
                float pre = acc[k][j];
                hidden1[j] = pre > 0f ? pre : 0f;
            }
            for (int j = 0; j < h2; j++) {
                float sum = b2[j];
                int offset = j * h1;
                for (int i = 0; i < h1; i++) {
                    sum += w2T[offset + i] * hidden1[i];
                }
                hidden2[j] = sum > 0f ? sum : 0f;
            }
            float logit = b3;
            for (int j = 0; j < h2; j++) {
                logit += w3[j] * hidden2[j];
            }
            result[k] = logit;
        }
        return result;
    }

    public float value(float[] obs) {
        float logit = logit(obs);
        return (float) (1.0 / (1.0 + Math.exp(-logit)));
    }
}
