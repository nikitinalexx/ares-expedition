package com.terraforming.ares.services.agent;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chooses with a trained model, one model per decision type (the same network shape, its own weights and its own
 * candidate-feature width). Configured by the JVM properties {@code ares.agent.model} (one model folder, kept for the
 * phase model) and {@code ares.agent.models} (a directory of folders, each a model; the decision type is read from the
 * model file). {@code ares.agent.temperature}: 0 = always the best candidate, otherwise sampling from
 * softmax(logit / temperature).
 * <p>
 * A decision type without a model is left to {@link DecisionAgent}. A model trained on data of another encoder is
 * refused at startup (schema hash), instead of playing garbage.
 */
@Service
public class NeuralPolicy {
    private final ConcurrentHashMap<DecisionType, MlpPhaseModel> models = new ConcurrentHashMap<>();
    /** Second model set ({@code -Dares.agent.models2}): what {@link com.terraforming.ares.model.PlayerDifficulty#AGENT_V2} plays. */
    private final ConcurrentHashMap<DecisionType, MlpPhaseModel> strongModels = new ConcurrentHashMap<>();
    private volatile double temperature;

    public NeuralPolicy() {
        double configured = Double.parseDouble(System.getProperty("ares.agent.temperature", "0"));
        String root = System.getProperty("ares.agent.models");
        if (root != null && !root.isBlank()) {
            loadInto(models, root, configured);
        }
        String strongRoot = System.getProperty("ares.agent.models2");
        if (strongRoot != null && !strongRoot.isBlank()) {
            loadInto(strongModels, strongRoot, configured);
        }
        String dir = System.getProperty("ares.agent.model");
        if (dir != null && !dir.isBlank()) {
            try {
                use(MlpPhaseModel.load(Path.of(dir)), configured);
            } catch (IOException e) {
                throw new UncheckedIOException("Can not load the agent model from " + dir, e);
            }
        }
    }

    private void loadInto(ConcurrentHashMap<DecisionType, MlpPhaseModel> target, String root, double configured) {
        try (var dirs = Files.list(Path.of(root))) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                if (Files.exists(dir.resolve("model.json"))) {
                    MlpPhaseModel model = MlpPhaseModel.load(dir);
                    checkSchema(model);
                    target.put(model.decisionType(), model);
                    this.temperature = configured;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Can not load the agent models from " + root, e);
        }
    }

    public void use(MlpPhaseModel newModel, double newTemperature) {
        checkSchema(newModel);
        if (newTemperature < 0) {
            throw new IllegalArgumentException("temperature must not be negative");
        }
        this.temperature = newTemperature;
        this.models.put(newModel.decisionType(), newModel);
    }

    private static void checkSchema(MlpPhaseModel model) {
        String expected = schema(model.decisionType()).hash();
        if (!expected.equals(model.schemaHash())) {
            throw new IllegalStateException("The model was trained on data with the schema " + model.schemaHash()
                    + " but the current encoder produces " + expected + ": the observation changed, the model must be trained again");
        }
    }

    public void clear() {
        this.models.clear();
    }

    public boolean hasModel() {
        return !models.isEmpty();
    }

    public boolean hasStrongModel() {
        return !strongModels.isEmpty();
    }

    /**
     * @return the index of the chosen candidate, or -1 when there is no model for this decision
     */
    public int choose(Decision decision) {
        return choose(decision, false);
    }

    /**
     * @param strongAnswer use the second model set ({@code ares.agent.models2}) when it has a model for this type
     * @return the index of the chosen candidate, or -1 when there is no model for this decision
     */
    public int choose(Decision decision, boolean strongAnswer) {
        MlpPhaseModel current = (strongAnswer && !strongModels.isEmpty() ? strongModels : models).get(decision.type());
        if (current == null) {
            AgentPerfStats.recordFallback();
            return -1;
        }
        long started = System.nanoTime();
        float[] logits = current.logits(decision.observation(), decision.candidates());
        int chosen = pick(logits, temperature);
        AgentPerfStats.recordNeural(System.nanoTime() - started, decision.candidates().size());
        return chosen;
    }

    /** Hash of every decision type's schema: the encoder's own view, for tooling (model conversion, audit). */
    public static java.util.Map<String, String> schemaHashes() {
        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
        for (DecisionType type : DecisionType.values()) {
            result.put(type.name(), schema(type).hash());
        }
        return result;
    }

    /** Package-private so tooling (e.g. {@link SyntheticModelFactory}) can build a model of the current encoder. */
    static DecisionFile.Schema schema(DecisionType type) {
        return switch (type) {
            case PICK_PHASE -> PickPhaseDecisions.schema();
            case PICK_CORPORATION -> CorporationDecisions.schema();
            case MULLIGAN -> MulliganDecisions.schema();
            case DRAFT_CARDS, DISCARD_CARDS, SELL_CARDS, SELL_CARDS_LAST_ROUND -> HandCardDecisions.schema(type);
            case STANDARD_ACTION -> StandardActionDecisions.schema();
            case BUILD_CARD -> BuildCardDecisions.schema();
            case BUILD_PAYMENT -> BuildPaymentDecisions.schema();
            case BUILD_INPUT -> BuildInputDecisions.schema();
            case PERFORM_BLUE_ACTION -> BlueActionDecisions.schema();
            case BLUE_INPUT -> BlueInputDecisions.schema();
            case UNMI_RT -> UnmiRtDecisions.schema();
            case COLLECT_INCOME -> CollectIncomeDecisions.schema();
        };
    }

    static int pick(float[] logits, double temperature) {
        int best = 0;
        for (int i = 1; i < logits.length; i++) {
            if (logits[i] > logits[best]) {
                best = i;
            }
        }
        if (temperature <= 0 || logits.length == 1) {
            return best;
        }
        double[] weights = new double[logits.length];
        double total = 0;
        for (int i = 0; i < logits.length; i++) {
            weights[i] = Math.exp((logits[i] - logits[best]) / temperature);
            total += weights[i];
        }
        double x = ThreadLocalRandom.current().nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            x -= weights[i];
            if (x <= 0) {
                return i;
            }
        }
        return best;
    }
}
