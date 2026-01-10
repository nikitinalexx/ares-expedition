package com.terraforming.ares.services.ai.turnProcessors.network2.buildParams;

import com.terraforming.ares.services.ai.dl4j.Prediction;

import java.util.List;

public class PredictionCursor {
    private final List<Prediction> predictions;
    private int pointer = 0;

    public PredictionCursor(List<Prediction> predictions) {
        this.predictions = predictions;
    }

    public Prediction next() {
        return predictions.get(pointer++);
    }

    public List<Prediction> next(int count) {
        List<Prediction> slice = predictions.subList(pointer, pointer + count);
        pointer += count;
        return slice;
    }

    public boolean hasRemaining() {
        return pointer < predictions.size();
    }

}

