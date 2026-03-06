package com.terraforming.ares.services.policyai.action;

import com.terraforming.ares.services.ai.dl4j.PolicyPrediction;

import java.util.List;

public final class ActionDistributionBuilder {

    public static ActionDistribution build(
            List<HeadAction> legalActions,
            PolicyPrediction prediction
    ) {
        float[] probs = new float[legalActions.size()];

        for (int i = 0; i < legalActions.size(); i++) {
            HeadAction action = legalActions.get(i);
            probs[i] = prediction.getActionProbability(
                    action.head(),
                    action.localIndex()
            );
        }

        normalize(probs);

        return new ActionDistribution(legalActions, probs, null);
    }

    private static void normalize(float[] arr) {
        float sum = 0f;
        for (float v : arr) sum += v;
        if (sum == 0f) return;

        for (int i = 0; i < arr.length; i++) {
            arr[i] /= sum;
        }
    }
}
