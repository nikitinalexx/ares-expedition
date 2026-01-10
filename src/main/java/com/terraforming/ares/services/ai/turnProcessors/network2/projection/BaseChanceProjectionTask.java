package com.terraforming.ares.services.ai.turnProcessors.network2.projection;

import com.terraforming.ares.services.ai.dl4j.Prediction;

import java.util.List;

public class BaseChanceProjectionTask implements ProjectionTask<Double> {
    private final List<float[]> data;
    private double chance;

    // Конструктор для одной проекции
    public BaseChanceProjectionTask(float[] singleProjection) {
        this.data = List.of(singleProjection);
    }

    @Override
    public List<float[]> getProjections() {
        return data;
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public void consumePredictions(List<Prediction> predictions) {
        this.chance = predictions.getFirst().baseProb;
    }

    @Override
    public Double getResults() {
        return chance;
    }
}
