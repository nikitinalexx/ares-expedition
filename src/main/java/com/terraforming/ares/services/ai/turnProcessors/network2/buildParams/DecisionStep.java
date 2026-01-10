package com.terraforming.ares.services.ai.turnProcessors.network2.buildParams;

import java.util.List;

public interface DecisionStep {
    void collectSimulations(List<float[]> simulations, DataCollectContext dataCollectContext);
    void applyPredictions(PredictionCursor cursor, OptimizedInputDecisions decisions);
    boolean isApplicable();
}
