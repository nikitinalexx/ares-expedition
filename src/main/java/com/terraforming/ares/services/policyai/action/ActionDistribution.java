package com.terraforming.ares.services.policyai.action;

import java.util.List;

public final class ActionDistribution {

    private final List<HeadAction> legalActions;
    private final float[] probabilities; // только для legalActions
    private final HeadAction chosen;

    public ActionDistribution(
            List<HeadAction> legalActions,
            float[] probabilities,
            HeadAction chosen
    ) {
        this.legalActions = legalActions;
        this.probabilities = probabilities;
        this.chosen = chosen;
    }

    public List<HeadAction> legalActions() { return legalActions; }
    public float[] probabilities() { return probabilities; }
    public HeadAction chosen() { return chosen; }
}
