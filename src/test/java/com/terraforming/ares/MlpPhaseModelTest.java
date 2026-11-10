package com.terraforming.ares;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.Candidate;
import com.terraforming.ares.services.agent.MlpPhaseModel;
import com.terraforming.ares.services.agent.NeuralPolicy;
import com.terraforming.ares.services.agent.PickPhaseDecisions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Java forward pass must give the numbers that the Python side exported (reference.json, computed by an independent numpy
 * implementation). A different normalisation, a different order of the weights or a different activation shows up here immediately.
 */
class MlpPhaseModelTest extends EngineTestBase {
    static final Path FIXTURE = Path.of("src", "test", "resources", "phase_model_fixture");

    @TempDir
    Path tmp;

    @Autowired
    NeuralPolicy neuralPolicy;
    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;

    private static List<Candidate> candidates(JsonNode cands) {
        List<Candidate> result = new ArrayList<>();
        for (int i = 0; i < cands.size(); i++) {
            float[] features = new float[cands.get(i).size()];
            for (int f = 0; f < features.length; f++) {
                features[f] = (float) cands.get(i).get(f).asDouble();
            }
            result.add(new Candidate(i + 1, features));
        }
        return result;
    }

    private static float[] floats(JsonNode array) {
        float[] values = new float[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = (float) array.get(i).asDouble();
        }
        return values;
    }

    @Test
    void javaGivesTheNumbersPythonExported() throws IOException {
        MlpPhaseModel model = MlpPhaseModel.load(FIXTURE);
        JsonNode reference = new ObjectMapper().readTree(FIXTURE.resolve("reference.json").toFile());
        assertTrue(reference.size() >= 20);

        double worst = 0;
        int compared = 0;
        for (JsonNode sample : reference) {
            float[] logits = model.logits(floats(sample.get("obs")), candidates(sample.get("cands")));
            assertEquals(sample.get("logits").size(), logits.length);
            for (int k = 0; k < logits.length; k++) {
                worst = Math.max(worst, Math.abs(logits[k] - sample.get("logits").get(k).asDouble()));
                compared++;
            }
        }
        assertTrue(compared > 40);
        assertTrue(worst < 1e-4, "largest difference to the numpy reference: " + worst);
    }

    @Test
    void theFixtureBelongsToTheCurrentEncoder() throws IOException {
        MlpPhaseModel model = MlpPhaseModel.load(FIXTURE);
        assertEquals(PickPhaseDecisions.schema().hash(), model.schemaHash(),
                "the encoder changed: regenerate the fixture with  python ml/make_test_fixture.py --schema-hash "
                        + PickPhaseDecisions.schema().hash() + " --out src/test/resources/phase_model_fixture");
        assertEquals(PickPhaseDecisions.schema().obsSize(), model.obsSize());
        assertEquals(PickPhaseDecisions.CANDIDATE_FEATURES, model.candFeatures());
    }

    @Test
    void aDamagedWeightsFileIsRefused() throws IOException {
        copyFixture(tmp);
        byte[] weights = Files.readAllBytes(tmp.resolve("weights.bin"));
        weights[weights.length / 2] ^= 0x40;
        Files.write(tmp.resolve("weights.bin"), weights);
        IOException e = assertThrows(IOException.class, () -> MlpPhaseModel.load(tmp));
        assertTrue(e.getMessage().contains("does not match"), e.getMessage());
    }

    @Test
    void aTruncatedWeightsFileIsRefused() throws IOException {
        copyFixture(tmp);
        byte[] weights = Files.readAllBytes(tmp.resolve("weights.bin"));
        Files.write(tmp.resolve("weights.bin"), java.util.Arrays.copyOf(weights, weights.length - 8));
        assertThrows(IOException.class, () -> MlpPhaseModel.load(tmp));
    }

    @Test
    void inputsOfTheWrongSizeAreRejectedInsteadOfBeingReadAsGarbage() throws IOException {
        MlpPhaseModel model = MlpPhaseModel.load(FIXTURE);
        List<Candidate> one = List.of(new Candidate(1, new float[PickPhaseDecisions.CANDIDATE_FEATURES]));
        assertThrows(IllegalArgumentException.class, () -> model.logits(new float[model.obsSize() - 1], one));
        assertThrows(IllegalArgumentException.class, () -> model.logits(new float[model.obsSize()], List.of(new Candidate(1, new float[3]))));
    }

    @Test
    void nanInTheObservationIsAnErrorNotAMove() throws IOException {
        MlpPhaseModel model = MlpPhaseModel.load(FIXTURE);
        float[] obs = new float[model.obsSize()];
        obs[3] = Float.NaN;
        assertThrows(IllegalStateException.class, () -> model.logits(obs, List.of(new Candidate(1, new float[PickPhaseDecisions.CANDIDATE_FEATURES]))));
    }

    @Test
    void aModelOfAnotherEncoderIsRefusedByThePolicy() throws IOException {
        copyFixture(tmp);
        String json = Files.readString(tmp.resolve("model.json")).replace(PickPhaseDecisions.schema().hash(), "0123456789abcdef");
        Files.writeString(tmp.resolve("model.json"), json);
        MlpPhaseModel stale = MlpPhaseModel.load(tmp);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> neuralPolicy.use(stale, 0));
        assertTrue(e.getMessage().contains("trained again"), e.getMessage());
        assertTrue(!neuralPolicy.hasModel());
    }

    @Test
    void temperatureZeroIsGreedyAndHigherTemperatureSpreadsTheChoices() throws IOException {
        MlpPhaseModel model = MlpPhaseModel.load(FIXTURE);
        JsonNode sample = new ObjectMapper().readTree(FIXTURE.resolve("reference.json").toFile()).get(0);
        float[] obs = floats(sample.get("obs"));
        List<Candidate> five = new ArrayList<>();
        for (int phase = 0; phase < 5; phase++) {
            float[] f = new float[PickPhaseDecisions.CANDIDATE_FEATURES];
            f[phase] = 1f;
            five.add(new Candidate(phase + 1, f));
        }
        float[] logits = model.logits(obs, five);
        int best = 0;
        for (int i = 1; i < 5; i++) {
            if (logits[i] > logits[best]) {
                best = i;
            }
        }

        neuralPolicy.use(model, 0);
        try {
            var decision = new com.terraforming.ares.services.agent.Decision(com.terraforming.ares.services.agent.DecisionType.PICK_PHASE, "x", obs, five);
            for (int i = 0; i < 20; i++) {
                assertEquals(best, neuralPolicy.choose(decision), "temperature 0 always takes the best candidate");
            }
            neuralPolicy.use(model, 50.0);
            java.util.Set<Integer> seen = new java.util.HashSet<>();
            for (int i = 0; i < 400; i++) {
                seen.add(neuralPolicy.choose(decision));
            }
            assertEquals(5, seen.size(), "a very high temperature explores all candidates");
        } finally {
            neuralPolicy.clear();
        }
    }

    @Test
    void anAgentPlaysCompleteGamesWithTheModel() throws IOException {
        neuralPolicy.use(MlpPhaseModel.load(FIXTURE), 0.5);
        try {
            for (int g = 0; g < 4; g++) {
                MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                        .playerNames(new ArrayList<>(List.of("A", "B")))
                        .computers(List.of(PlayerDifficulty.AGENT, g % 2 == 0 ? PlayerDifficulty.SMART : PlayerDifficulty.AGENT_R))
                        .mulligan(true)
                        .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                        .dummyHand(true).build());
                simulation.processSimulation(game);
                assertEquals(com.terraforming.ares.model.StateType.GAME_END, game.getStateType());
            }
        } finally {
            neuralPolicy.clear();
        }
    }

    private static void copyFixture(Path target) throws IOException {
        try (var files = Files.list(FIXTURE)) {
            for (Path f : files.toList()) {
                Files.copy(f, target.resolve(f.getFileName()));
            }
        }
    }
}
