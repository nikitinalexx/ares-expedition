package com.terraforming.ares.services.ai.turnProcessors.network2.projection;

import com.terraforming.ares.services.ai.dl4j.Prediction;

import java.util.List;

public class ScenariosBatchTask implements  ProjectionTask<List<Scenario>>{
    private final List<Scenario> scenarios;
    public ScenariosBatchTask(List<Scenario> scenarios) {
        this.scenarios = scenarios;
    }
    @Override
    public List<float[]> getProjections() {
        return scenarios.stream().map(Scenario::getFinalStateData).toList();
    }

    @Override
    public void consumePredictions(List<Prediction> predictions) {
        for (int i = 0; i < scenarios.size(); i++) {
            scenarios.get(i).setFinalChance(predictions.get(i).baseProb);
        }
    }

    @Override
    public List<Scenario> getResults() {
        return scenarios;
    }

    @Override
    public int size() {
        return scenarios.size();
    }

}
