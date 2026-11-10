package com.terraforming.ares.services.agent;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chooses with a trained model. Configured by the JVM properties {@code ares.agent.model} (folder of the model) and
 * {@code ares.agent.temperature} (0 = always the best candidate, otherwise sampling from softmax(logit / temperature)).
 * Without a model it answers nothing and the plain {@link DecisionAgent} decides.
 * <p>
 * A model trained on data of another encoder is refused at startup (schema hash), instead of playing garbage.
 */
@Service
public class NeuralPolicy {
    private volatile MlpPhaseModel model;
    private volatile double temperature;

    public NeuralPolicy() {
        String dir = System.getProperty("ares.agent.model");
        if (dir != null && !dir.isBlank()) {
            try {
                use(MlpPhaseModel.load(Path.of(dir)), Double.parseDouble(System.getProperty("ares.agent.temperature", "0")));
            } catch (IOException e) {
                throw new UncheckedIOException("Can not load the agent model from " + dir, e);
            }
        }
    }

    public void use(MlpPhaseModel newModel, double newTemperature) {
        String expected = PickPhaseDecisions.schema().hash();
        if (!expected.equals(newModel.schemaHash())) {
            throw new IllegalStateException("The model was trained on data with the schema " + newModel.schemaHash()
                    + " but the current encoder produces " + expected + ": the observation changed, the model must be trained again");
        }
        if (newTemperature < 0) {
            throw new IllegalArgumentException("temperature must not be negative");
        }
        this.temperature = newTemperature;
        this.model = newModel;
    }

    public void clear() {
        this.model = null;
    }

    public boolean hasModel() {
        return model != null;
    }

    /**
     * @return the index of the chosen candidate, or -1 when there is no model for this decision
     */
    public int choose(Decision decision) {
        MlpPhaseModel current = model;
        if (current == null || decision.type() != DecisionType.PICK_PHASE) {
            return -1;
        }
        float[] logits = current.logits(decision.observation(), decision.candidates());
        return pick(logits, temperature);
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
