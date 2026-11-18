package com.terraforming.ares;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.terraforming.ares.services.agent.MlpValueModel;
import com.terraforming.ares.services.agent.ObservationBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Is the critic's matrix-vector product already at the compiler's limit?
 * <p>
 * The three critics are the largest per-candidate cost (measured 52-64%), and 84% of their work is the 1256x512 first layer.
 * Before adding a GPU dependency, this measures the current loop against two hand-unrolled variants on the same weights:
 * if the compiler already vectorises, the variants change nothing and the GPU/queue path is the only remaining lever.
 * <p>
 * The variants are written here, not in the model, because this test is a decision instrument: it prints the numbers and
 * asserts only that all three compute the same dot product within a tolerance that cannot flip a decision on a
 * well-separated menu (1e-4 on a logit whose typical spread is ~1).
 */
class MatvecVariantsBenchTest {

    @Test
    void comparesTheCurrentMatvecWithUnrolledVariants() throws IOException {
        int obs = ObservationBuilder.size();
        int h1 = 512, h2 = 256;
        long seed = 7L;
        Random random = new Random(seed);

        float[] mean = new float[obs], std = new float[obs];
        float[] w1 = new float[h1 * obs], b1 = new float[h1];
        float[] w2 = new float[h2 * h1], b2 = new float[h2];
        float[] w3 = new float[h2];
        float b3;
        for (int i = 0; i < obs; i++) {
            mean[i] = (random.nextFloat() - 0.5f) * 2f;
            std[i] = 0.2f + random.nextFloat() * 2f;
        }
        for (int i = 0; i < w1.length; i++) {
            w1[i] = (random.nextFloat() - 0.5f) * 0.2f;
        }
        for (int i = 0; i < b1.length; i++) {
            b1[i] = (random.nextFloat() - 0.5f) * 0.1f;
        }
        for (int i = 0; i < w2.length; i++) {
            w2[i] = (random.nextFloat() - 0.5f) * 0.2f;
        }
        for (int i = 0; i < b2.length; i++) {
            b2[i] = (random.nextFloat() - 0.5f) * 0.1f;
        }
        for (int i = 0; i < w3.length; i++) {
            w3[i] = (random.nextFloat() - 0.5f) * 0.2f;
        }
        b3 = (random.nextFloat() - 0.5f) * 0.1f;

        MlpValueModel model = writeModel(h1, h2, mean, std, w1, b1, w2, b2, w3, b3);
        float[] observation = new float[obs];
        for (int i = 0; i < obs; i++) {
            observation[i] = random.nextFloat() * 4f;
        }

        Buffers buffers = new Buffers(h1, h2, obs);
        int rounds = 2000;
        for (int i = 0; i < 300; i++) {           // warm up the JIT
            model.logit(observation);
            unrolled(observation, mean, std, w1, b1, w2, b2, w3, b3, h1, h2, 4, buffers);
            unrolled(observation, mean, std, w1, b1, w2, b2, w3, b3, h1, h2, 8, buffers);
        }

        long current = time(() -> model.logit(observation), rounds);
        long four = time(() -> unrolled(observation, mean, std, w1, b1, w2, b2, w3, b3, h1, h2, 4, buffers), rounds);
        long eight = time(() -> unrolled(observation, mean, std, w1, b1, w2, b2, w3, b3, h1, h2, 8, buffers), rounds);
        long jFour = time(() -> unrolledJ(observation, mean, std, w1, b1, w2, b2, w3, b3, h1, h2, 4, buffers), rounds);
        long jEight = time(() -> unrolledJ(observation, mean, std, w1, b1, w2, b2, w3, b3, h1, h2, 8, buffers), rounds);

        double base = current / 1e3 / rounds;
        System.out.printf("matvec per critic: current %.1f us, unrolled-i4 %.1f us (%.2fx), unrolled-i8 %.1f us (%.2fx), "
                        + "unrolled-j4 %.1f us (%.2fx), unrolled-j8 %.1f us (%.2fx)%n",
                base, four / 1e3 / rounds, (double) current / four, eight / 1e3 / rounds, (double) current / eight,
                jFour / 1e3 / rounds, (double) current / jFour, jEight / 1e3 / rounds, (double) current / jEight);

        float reference = model.logit(observation);
        // the j-unrolled variant must be BIT-identical: the adoption criterion for a no-retrain speedup
        assertEquals(Float.floatToIntBits(reference),
                Float.floatToIntBits(unrolledJ(observation, mean, std, w1, b1, w2, b2, w3, b3, h1, h2, 4, buffers)),
                "unrolled-j4 must be bit-identical to the model's own loop");
        assertEquals(Float.floatToIntBits(reference),
                Float.floatToIntBits(unrolledJ(observation, mean, std, w1, b1, w2, b2, w3, b3, h1, h2, 8, buffers)),
                "unrolled-j8 must be bit-identical to the model's own loop");
        assertTrue(Math.abs(reference - unrolled(observation, mean, std, w1, b1, w2, b2, w3, b3, h1, h2, 4, buffers)) < 1e-3,
                "the unrolled variant must compute the same function");
        assertTrue(Math.abs(reference - unrolled(observation, mean, std, w1, b1, w2, b2, w3, b3, h1, h2, 8, buffers)) < 1e-3,
                "the 8-way variant must compute the same function");
    }

    /** Buffers reused across calls, so the measurement is the arithmetic and not the allocator. */
    private static final class Buffers {
        final float[] h1;
        final float[] h2;
        final float[] z;

        Buffers(int h1, int h2, int features) {
            this.h1 = new float[h1];
            this.h2 = new float[h2];
            this.z = new float[features];
        }
    }

    /** The same forward pass with `lanes` independent accumulators over the features. */
    private static float unrolled(float[] obs, float[] mean, float[] std, float[] w1, float[] b1,
                                  float[] w2, float[] b2, float[] w3, float b3, int h1, int h2, int lanes,
                                  Buffers buffers) {
        int features = obs.length;
        float[] h = buffers.h1;
        System.arraycopy(b1, 0, h, 0, h1);
        float[] z = buffers.z;
        for (int i = 0; i < features; i++) {
            float v = (obs[i] - mean[i]) / std[i];
            z[i] = v < -1e6f ? -1e6f : Math.min(v, 1e6f);
        }
        int i = 0;
        for (; i + lanes <= features; i += lanes) {
            for (int j = 0; j < h1; j++) {
                float sum = h[j];
                int base = j * features;
                for (int l = 0; l < lanes; l++) {
                    sum += w1[base + i + l] * z[i + l];
                }
                h[j] = sum;
            }
        }
        for (; i < features; i++) {
            int base = 0;
            for (int j = 0; j < h1; j++) {
                h[j] += w1[j * features + i] * z[i];
            }
        }
        float[] h2v = buffers.h2;
        System.arraycopy(b2, 0, h2v, 0, h2);
        for (int k = 0; k < h2; k++) {
            float sum = h2v[k];
            int base = k * h1;
            for (int j = 0; j < h1; j++) {
                if (h[j] > 0f) {
                    sum += w2[base + j] * h[j];
                }
            }
            h2v[k] = sum;
        }
        float out = b3;
        for (int k = 0; k < h2; k++) {
            if (h2v[k] > 0f) {
                out += w3[k] * h2v[k];
            }
        }
        return out;
    }

    /**
     * The same arithmetic as the model's own loop, with the INNER loop over hidden units unrolled.
     * <p>
     * This is the variant that can be adopted without any decision change: each hidden unit still receives exactly one
     * `+= w * z` per feature, in the same order, so every value is bit-identical to `MlpValueModel.logit`. What changes is
     * only that four (or eight) neighbouring units are updated per loaded feature, which gives the compiler independent
     * operations to vectorise and keeps the contiguous access pattern of `w1` (row per feature).
     */
    private static float unrolledJ(float[] obs, float[] mean, float[] std, float[] w1, float[] b1,
                                   float[] w2, float[] b2, float[] w3, float b3, int h1, int h2, int lanes,
                                   Buffers buffers) {
        int features = obs.length;
        float[] h = buffers.h1;
        System.arraycopy(b1, 0, h, 0, h1);
        float[] z = buffers.z;
        for (int i = 0; i < features; i++) {
            float v = (obs[i] - mean[i]) / std[i];
            z[i] = v < -1e6f ? -1e6f : Math.min(v, 1e6f);
        }
        int j = 0;
        for (; j + lanes <= h1; j += lanes) {
            for (int i = 0; i < features; i++) {
                int base = i * h1 + j;
                float zi = z[i];
                for (int l = 0; l < lanes; l++) {
                    h[j + l] += w1[base + l] * zi;
                }
            }
        }
        for (; j < h1; j++) {
            for (int i = 0; i < features; i++) {
                h[j] += w1[i * h1 + j] * z[i];
            }
        }
        float[] h2v = buffers.h2;
        System.arraycopy(b2, 0, h2v, 0, h2);
        int k = 0;
        for (; k + lanes <= h2; k += lanes) {
            for (int jj = 0; jj < h1; jj++) {
                float hv = h[jj] > 0f ? h[jj] : 0f;
                if (hv == 0f) {
                    continue;
                }
                int base = k * h1 + jj;
                for (int l = 0; l < lanes; l++) {
                    h2v[k + l] += w2[base + l * h1] * hv;
                }
            }
        }
        for (; k < h2; k++) {
            float sum = h2v[k];
            int base = k * h1;
            for (int jj = 0; jj < h1; jj++) {
                if (h[jj] > 0f) {
                    sum += w2[base + jj] * h[jj];
                }
            }
            h2v[k] = sum;
        }
        float out = b3;
        for (int kk = 0; kk < h2; kk++) {
            if (h2v[kk] > 0f) {
                out += w3[kk] * h2v[kk];
            }
        }
        return out;
    }

    private static long time(Runnable body, int rounds) {
        long start = System.nanoTime();
        for (int i = 0; i < rounds; i++) {
            body.run();
        }
        return System.nanoTime() - start;
    }

    private MlpValueModel writeModel(int h1, int h2, float[] mean, float[] std, float[] w1, float[] b1,
                                     float[] w2, float[] b2, float[] w3, float b3) throws IOException {
        int obs = mean.length;
        ByteBuffer buffer = ByteBuffer.allocate(4 * (2 * obs + h1 * obs + h1 + h2 * h1 + h2 + h2 + 1))
                .order(ByteOrder.LITTLE_ENDIAN);
        for (float v : mean) buffer.putFloat(v);
        for (float v : std) buffer.putFloat(v);
        for (float v : w1) buffer.putFloat(v);
        for (float v : b1) buffer.putFloat(v);
        for (float v : w2) buffer.putFloat(v);
        for (float v : b2) buffer.putFloat(v);
        for (float v : w3) buffer.putFloat(v);
        buffer.putFloat(b3);
        byte[] weights = buffer.array();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode meta = mapper.createObjectNode();
        meta.put("kind", "value_mlp");
        meta.put("version", 1);
        meta.put("obsSize", obs);
        meta.put("observationHash", "test");
        ArrayNode hidden = meta.putArray("hidden");
        hidden.add(h1);
        hidden.add(h2);
        meta.put("clip", 1e6);
        meta.put("weightsCount", weights.length / 4);
        meta.put("weightsSha256", sha256(weights));
        Path dir = modelDir.resolve("m");
        Files.createDirectories(dir);
        Files.write(dir.resolve("weights.bin"), weights);
        mapper.writerWithDefaultPrettyPrinter().writeValue(dir.resolve("model.json").toFile(), meta);
        return MlpValueModel.load(dir);
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @TempDir
    Path modelDir;
}
