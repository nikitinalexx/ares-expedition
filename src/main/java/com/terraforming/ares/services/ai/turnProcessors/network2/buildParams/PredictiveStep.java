package com.terraforming.ares.services.ai.turnProcessors.network2.buildParams;

import java.util.List;

public interface PredictiveStep extends DecisionStep {
    void collectSimulations(List<float[]> simulations);
    void applyPredictions(PredictionCursor cursor, OptimizedInputDecisions decisions);
}
