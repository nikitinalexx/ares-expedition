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
    @Autowired
    com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService baseEncoder;

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

        long copyNanos = 0, obsNanos = 0, publicNanos = 0, criticNanos = 0;
        for (int i = 0; i < rounds; i++) {
            long t0 = System.nanoTime();
            MarsGame copy = new MarsGame(game);
            long t1 = System.nanoTime();
            float[] obs = observationBuilder.build(copy, copy.getPlayerByUuid(player.getUuid()));
            long t2 = System.nanoTime();
            // with --search.symmetric every candidate also builds the OPPONENT's public view; this is that second build
            Player opponent = copy.getPlayerUuidToPlayer().values().stream()
                    .filter(p -> !p.getUuid().equals(player.getUuid())).findFirst().orElseThrow();
            float[] other = observationBuilder.buildPublic(copy, opponent);
            long t2b = System.nanoTime();
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
            publicNanos += t2b - t2;
            criticNanos += t3 - t2b;
        }

        double copy = copyNanos / 1e3 / rounds, obs = obsNanos / 1e3 / rounds;
        double pub = publicNanos / 1e3 / rounds, critic = criticNanos / 1e3 / rounds;
        double total = copy + obs + pub + critic;
        System.out.printf("per candidate: copy %.1f us, observation %.1f us, opponent public view %.1f us, "
                        + "three critics %.1f us, total %.1f us (copy %.0f%%, obs %.0f%%, public %.0f%%, critics %.0f%%)%n",
                copy, obs, pub, critic, total,
                100 * copy / total, 100 * obs / total, 100 * pub / total, 100 * critic / total);
        assertTrue(copy > 0 && obs > 0 && critic > 0, "every path must have been measured");
    }

    /**
     * How much of the observation cost is the legacy V2 block?
     * <p>
     * The 1256-value vector is the current encoder's columns *plus* a legacy block kept only because NETWORK_V2 was trained
     * on it (`ObservationBuilder` calls {@code baseEncoder.collectData}). If that block is most of the 86 us, dropping it
     * from the encoder would speed up every decision far more than any micro-optimisation - at the price of retraining the
     * critics on the narrower observation.
     */
    @Test
    void printsTheShareOfTheLegacyEncoderBlock() {
        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true)
                .build());
        Player player = game.getPlayerUuidToPlayer().values().iterator().next();

        int rounds = 2000;
        for (int i = 0; i < 300; i++) {
            baseEncoder.collectData(game, player.getUuid());
            observationBuilder.build(game, player);
            observationBuilder.buildPublic(game, player);
        }
        long legacy = 0, mine = 0, publicView = 0;
        for (int i = 0; i < rounds; i++) {
            long t0 = System.nanoTime();
            float[] block = baseEncoder.collectData(game, player.getUuid());
            long t1 = System.nanoTime();
            observationBuilder.build(game, player);
            long t2 = System.nanoTime();
            observationBuilder.buildPublic(game, player);
            long t3 = System.nanoTime();
            if (block.length == 0) {
                throw new IllegalStateException("the legacy encoder produced nothing");
            }
            legacy += t1 - t0;
            mine += t2 - t1;
            publicView += t3 - t2;
        }
        double l = legacy / 1e3 / rounds, b = mine / 1e3 / rounds, p = publicView / 1e3 / rounds;
        System.out.printf("legacy V2 block %.1f us, build() %.1f us (of which legacy %.0f%%), buildPublic() %.1f us%n",
                l, b, 100 * l / b, p);
        assertTrue(l > 0 && b > 0, "both paths must have been measured");
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
