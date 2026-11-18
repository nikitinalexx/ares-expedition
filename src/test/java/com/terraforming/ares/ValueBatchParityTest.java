package com.terraforming.ares;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.agent.MlpValueModel;
import com.terraforming.ares.services.agent.ObservationBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The batched forward pass must return exactly what the single-row forward pass returns.
 * <p>
 * Why this test and not a thousand games: `batchLogits` is an optimisation that reorders the arithmetic (it accumulates the
 * whole menu feature by feature and skips columns that are zero across the batch), so a wrong implementation would change
 * decisions silently - the worst kind of bug for a ranker. The owner's rule for this work is a zero tolerance, so the
 * assertion is {@code Float.compare == 0}, not an epsilon.
 * <p>
 * The model is written by the test in the exact {@code value_mlp} v1 format the trainer exports (a real bundle lives
 * outside the repository and a test that skips when it is missing proves nothing).
 */
class ValueBatchParityTest extends EngineTestBase {

    @Autowired
    GameService gameService;
    @Autowired
    ObservationBuilder observationBuilder;

    @Test
    void batchedForwardPassEqualsTheSingleRowOneBitForBit() throws IOException {
        MlpValueModel model = loadSyntheticModel(4, 3, 42L);
        List<float[]> observations = observations(60);

        for (float[] obs : observations) {
            float single = model.logit(obs);
            float batched = model.batchLogits(new float[][]{obs}, 1)[0];
            assertEquals(Float.floatToIntBits(single), Float.floatToIntBits(batched),
                    "a one-row batch must be the single-row result, bit for bit");
        }

        for (int size = 2; size <= 8; size++) {
            for (int start = 0; start + size <= observations.size(); start += size) {
                float[][] batch = observations.subList(start, start + size).toArray(new float[0][]);
                float[] batched = model.batchLogits(batch, size);
                for (int i = 0; i < size; i++) {
                    float single = model.logit(batch[i]);
                    assertEquals(Float.floatToIntBits(single), Float.floatToIntBits(batched[i]),
                            "batch of " + size + ", row " + i + " diverges from the single-row path");
                }
            }
        }
    }

    /** The same, on the shapes that actually stress the sparse skip: all-zero, one-hot, and dense rows. */
    @Test
    void sparseAndDenseRowsAgree() throws IOException {
        MlpValueModel model = loadSyntheticModel(6, 4, 7L);
        int size = ObservationBuilder.size();
        List<float[]> observations = new ArrayList<>();
        observations.add(new float[size]);                                   // all zero
        for (int column : new int[]{0, 17, 89, size / 2, size - 1}) {
            float[] oneHot = new float[size];
            oneHot[column] = 1f;
            observations.add(oneHot);
        }
        Random random = new Random(11);
        for (int k = 0; k < 6; k++) {
            float[] dense = new float[size];
            for (int i = 0; i < size; i++) {
                dense[i] = random.nextFloat() * 100f;
            }
            observations.add(dense);
        }

        float[][] batch = observations.toArray(new float[0][]);
        float[] batched = model.batchLogits(batch, batch.length);
        for (int i = 0; i < batch.length; i++) {
            assertEquals(Float.floatToIntBits(model.logit(batch[i])), Float.floatToIntBits(batched[i]),
                    "row " + i + " of a mixed sparse/dense batch diverges");
        }
    }

    /** Not an assertion: the measured ratio, so the speed decision has a number next to the correctness one. */
    @Test
    void batchIsNotSlowerThanTheSingleRowLoop() throws IOException {
        MlpValueModel model = loadSyntheticModel(64, 32, 3L);
        List<float[]> observations = observations(512);
        float[][] batch = observations.toArray(new float[0][]);

        // warm up both paths
        for (int i = 0; i < 20; i++) {
            model.logit(batch[i]);
            model.batchLogits(batch, batch.length);
        }

        long singleStart = System.nanoTime();
        float sum = 0f;
        for (int round = 0; round < 20; round++) {
            for (float[] obs : batch) {
                sum += model.logit(obs);
            }
        }
        long singleNanos = System.nanoTime() - singleStart;

        long batchStart = System.nanoTime();
        float batchSum = 0f;
        for (int round = 0; round < 20; round++) {
            for (float value : model.batchLogits(batch, batch.length)) {
                batchSum += value;
            }
        }
        long batchNanos = System.nanoTime() - batchStart;

        System.out.printf("single %.1f us/row, batched %.1f us/row, ratio %.2fx%n",
                singleNanos / 1e3 / (20.0 * batch.length), batchNanos / 1e3 / (20.0 * batch.length),
                (double) singleNanos / batchNanos);
        assertTrue(Math.abs(sum - batchSum) < 1e-2, "the two paths must agree in total as well");
    }

    // ------------------------------------------------------------------------------------------------------------------

    /** Observations of a real game: the sparse, card-identity-heavy shapes the search actually feeds the critic. */
    private List<float[]> observations(int count) {
        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true)
                .build());
        List<float[]> result = new ArrayList<>();
        for (Player player : game.getPlayerUuidToPlayer().values()) {
            float[] base = observationBuilder.build(game, player);
            result.add(base.clone());
            float[] withNoise = base.clone();
            Random random = new Random(player.getUuid().hashCode());
            for (int i = 0; i < withNoise.length; i++) {
                if (random.nextInt(40) == 0) {
                    withNoise[i] += random.nextFloat();
                }
            }
            result.add(withNoise);
        }
        // pad up to `count` with deterministic perturbations of the observed rows so the batches are varied
        Random random = new Random(5);
        while (result.size() < count) {
            float[] source = result.get(random.nextInt(result.size())).clone();
            for (int i = 0; i < source.length; i++) {
                if (random.nextInt(25) == 0) {
                    source[i] = random.nextFloat() * 3f;
                }
            }
            result.add(source);
        }
        return result.subList(0, count);
    }

    /** Writes a value model in the {@code value_mlp} v1 format with deterministic, finite weights. */
    private MlpValueModel loadSyntheticModel(int h1, int h2, long seed) throws IOException {
        int obs = ObservationBuilder.size();
        Random random = new Random(seed);
        ByteBuffer buffer = ByteBuffer.allocate(4 * (2 * obs + h1 * obs + h1 + h2 * h1 + h2 + h2 + 1))
                .order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < obs; i++) {
            buffer.putFloat((random.nextFloat() - 0.5f) * 2f);          // mean
        }
        for (int i = 0; i < obs; i++) {
            buffer.putFloat(0.2f + random.nextFloat() * 2f);            // std, strictly positive
        }
        for (int i = 0; i < h1 * obs; i++) {
            buffer.putFloat((random.nextFloat() - 0.5f) * 0.2f);        // w1
        }
        for (int i = 0; i < h1; i++) {
            buffer.putFloat((random.nextFloat() - 0.5f) * 0.1f);        // b1
        }
        for (int i = 0; i < h2 * h1; i++) {
            buffer.putFloat((random.nextFloat() - 0.5f) * 0.2f);        // w2
        }
        for (int i = 0; i < h2; i++) {
            buffer.putFloat((random.nextFloat() - 0.5f) * 0.1f);        // b2
        }
        for (int i = 0; i < h2; i++) {
            buffer.putFloat((random.nextFloat() - 0.5f) * 0.2f);        // w3
        }
        buffer.putFloat((random.nextFloat() - 0.5f) * 0.1f);            // b3

        byte[] weights = buffer.array();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode meta = mapper.createObjectNode();
        meta.put("kind", "value_mlp");
        meta.put("version", 1);
        meta.put("obsSize", obs);
        ArrayNode hidden = meta.putArray("hidden");
        hidden.add(h1);
        hidden.add(h2);
        meta.put("clip", 1e6);
        // the loader refuses a weights.bin whose size or digest does not match model.json (half-written export)
        meta.put("weightsCount", weights.length / 4);
        meta.put("weightsSha256", sha256(weights));
        // the loader validates the observation WIDTH, not the hash string; the hash is informational here
        meta.put("observationHash", "test");
        Path dir = modelDir;
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
