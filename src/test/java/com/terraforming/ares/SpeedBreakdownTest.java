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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where the per-candidate milliseconds actually go: copy the game, build the observation, run the critics.
 * <p>
 * The search does all three for every candidate of every menu (~7.6 candidates, ~15.6 million decisions in a 20 000-game
 * corpus), so a 5% cut of the largest term is worth more than a clever optimisation of the smallest. No timing assertion:
 * the numbers are printed, and the choice of the next lever is made from them.
 */
class SpeedBreakdownTest extends EngineTestBase {

    @Autowired
    GameService gameService;
    @Autowired
    ObservationBuilder observationBuilder;

    @Test
    void printsThePerCandidateCostOfCopyObservationAndCritic() throws IOException {
        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true)
                .build());
        Player player = game.getPlayerUuidToPlayer().values().iterator().next();
        List<MlpValueModel> critics = List.of(model(1), model(2), model(3));

        int rounds = 2000;
        // warm up the JIT on every path before measuring
        for (int i = 0; i < 200; i++) {
            MarsGame copy = new MarsGame(game);
            float[] obs = observationBuilder.build(copy, copy.getPlayerByUuid(player.getUuid()));
            for (MlpValueModel critic : critics) {
                critic.logit(obs);
            }
        }

        long copyNanos = 0, obsNanos = 0, criticNanos = 0;
        for (int i = 0; i < rounds; i++) {
            long t0 = System.nanoTime();
            MarsGame copy = new MarsGame(game);
            long t1 = System.nanoTime();
            float[] obs = observationBuilder.build(copy, copy.getPlayerByUuid(player.getUuid()));
            long t2 = System.nanoTime();
            float sum = 0f;
            for (MlpValueModel critic : critics) {
                sum += critic.logit(obs);
            }
            long t3 = System.nanoTime();
            if (!Float.isFinite(sum)) {
                throw new IllegalStateException("the critic produced a non-finite value");
            }
            copyNanos += t1 - t0;
            obsNanos += t2 - t1;
            criticNanos += t3 - t2;
        }

        double copy = copyNanos / 1e3 / rounds, obs = obsNanos / 1e3 / rounds, critic = criticNanos / 1e3 / rounds;
        System.out.printf("per candidate: copy %.1f us, observation %.1f us, three critics %.1f us, total %.1f us "
                        + "(copy %.0f%%, observation %.0f%%, critics %.0f%%)%n",
                copy, obs, critic, copy + obs + critic,
                100 * copy / (copy + obs + critic), 100 * obs / (copy + obs + critic),
                100 * critic / (copy + obs + critic));
        assertTrue(copy > 0 && obs > 0 && critic > 0, "every path must have been measured");
    }

    private MlpValueModel model(long seed) throws IOException {
        int obs = ObservationBuilder.size();
        int h1 = 512, h2 = 256;
        Random random = new Random(seed);
        ByteBuffer buffer = ByteBuffer.allocate(4 * (2 * obs + h1 * obs + h1 + h2 * h1 + h2 + h2 + 1))
                .order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < obs; i++) {
            buffer.putFloat((random.nextFloat() - 0.5f) * 2f);
        }
        for (int i = 0; i < obs; i++) {
            buffer.putFloat(0.2f + random.nextFloat() * 2f);
        }
        for (int i = 0; i < h1 * obs; i++) {
            buffer.putFloat((random.nextFloat() - 0.5f) * 0.2f);
        }
        for (int i = 0; i < h1; i++) {
            buffer.putFloat((random.nextFloat() - 0.5f) * 0.1f);
        }
        for (int i = 0; i < h2 * h1; i++) {
            buffer.putFloat((random.nextFloat() - 0.5f) * 0.2f);
        }
        for (int i = 0; i < h2; i++) {
            buffer.putFloat((random.nextFloat() - 0.5f) * 0.1f);
        }
        for (int i = 0; i < h2; i++) {
            buffer.putFloat((random.nextFloat() - 0.5f) * 0.2f);
        }
        buffer.putFloat((random.nextFloat() - 0.5f) * 0.1f);

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
        meta.put("observationHash", "test");
        meta.put("weightsCount", weights.length / 4);
        meta.put("weightsSha256", sha256(weights));
        Path dir = modelDir.resolve("m" + seed);
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
