package com.terraforming.ares.services.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
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
 */
public final class MlpPhaseModel {
    private final DecisionType decisionType;
    private final String schemaHash;
    private final int obsSize;
    private final int candFeatures;
    private final int h1;
    private final int h2;
    private final float clip;
    private final float[] mean;
    private final float[] std;
    private final float[] wObs;
    private final float[] bObs;
    private final float[] wCand;
    private final float[] wMid;
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
        this.obsSize = meta.get("obsSize").asInt();
        this.candFeatures = meta.get("candFeatures").asInt();
        this.h1 = meta.get("hidden").get(0).asInt();
        this.h2 = meta.get("hidden").get(1).asInt();
        this.clip = (float) meta.get("clip").asDouble();
        this.mean = read(weights, obsSize);
        this.std = read(weights, obsSize);
        this.wObs = read(weights, h1 * obsSize);
        this.bObs = read(weights, h1);
        this.wCand = read(weights, h1 * candFeatures);
        this.wMid = read(weights, h2 * h1);
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

    public DecisionType decisionType() {
        return decisionType;
    }

    public String schemaHash() {
        return schemaHash;
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
        float[] z = new float[obsSize];
        for (int i = 0; i < obsSize; i++) {
            float v = (obs[i] - mean[i]) / std[i];
            z[i] = v < -clip ? -clip : Math.min(v, clip);
        }
        double[] a = new double[h1];
        for (int j = 0; j < h1; j++) {
            double sum = bObs[j];
            int row = j * obsSize;
            for (int i = 0; i < obsSize; i++) {
                sum += wObs[row + i] * z[i];
            }
            a[j] = sum;
        }
        float[] result = new float[candidates.size()];
        double[] hidden1 = new double[h1];
        for (int k = 0; k < result.length; k++) {
            float[] c = candidates.get(k).features();
            if (c.length != candFeatures) {
                throw new IllegalArgumentException("Candidate has " + c.length + " features, the model expects " + candFeatures);
            }
            for (int j = 0; j < h1; j++) {
                double sum = a[j];
                int row = j * candFeatures;
                for (int f = 0; f < candFeatures; f++) {
                    sum += wCand[row + f] * c[f];
                }
                hidden1[j] = Math.max(sum, 0.0);
            }
            double logit = bOut;
            for (int j = 0; j < h2; j++) {
                double sum = bMid[j];
                int row = j * h1;
                for (int i = 0; i < h1; i++) {
                    sum += wMid[row + i] * hidden1[i];
                }
                logit += wOut[j] * Math.max(sum, 0.0);
            }
            if (!Double.isFinite(logit)) {
                throw new IllegalStateException("The model produced " + logit + " (NaN/Inf in the input?)");
            }
            result[k] = (float) logit;
        }
        return result;
    }
}
