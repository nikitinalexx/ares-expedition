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
    private volatile double temperature;

    public NeuralPolicy() {
        double configured = Double.parseDouble(System.getProperty("ares.agent.temperature", "0"));
        String root = System.getProperty("ares.agent.models");
        if (root != null && !root.isBlank()) {
            try (var dirs = Files.list(Path.of(root))) {
                for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                    if (Files.exists(dir.resolve("model.json"))) {
                        use(MlpPhaseModel.load(dir), configured);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Can not load the agent models from " + root, e);
            }
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

    public void use(MlpPhaseModel newModel, double newTemperature) {
        String expected = schema(newModel.decisionType()).hash();
        if (!expected.equals(newModel.schemaHash())) {
            throw new IllegalStateException("The model was trained on data with the schema " + newModel.schemaHash()
                    + " but the current encoder produces " + expected + ": the observation changed, the model must be trained again");
        }
        if (newTemperature < 0) {
            throw new IllegalArgumentException("temperature must not be negative");
        }
        this.temperature = newTemperature;
        this.models.put(newModel.decisionType(), newModel);
    }

    public void clear() {
        this.models.clear();
    }

    public boolean hasModel() {
        return !models.isEmpty();
    }

    /**
     * @return the index of the chosen candidate, or -1 when there is no model for this decision
     */
    public int choose(Decision decision) {
        MlpPhaseModel current = models.get(decision.type());
        if (current == null) {
            return -1;
        }
        float[] logits = current.logits(decision.observation(), decision.candidates());
        return pick(logits, temperature);
    }

    private static DecisionFile.Schema schema(DecisionType type) {
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
