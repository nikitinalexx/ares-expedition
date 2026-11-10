package com.terraforming.ares.services.agent;

/**
 * Kinds of decisions the learnable agent takes over from the baseline computer. Every other decision is still played by
 * the fallback difficulty (SMART), so the agent can be introduced and measured one decision at a time.
 */
public enum DecisionType {
    PICK_PHASE(5, PickPhaseDecisions.OBSERVATION_LAYOUT);

    private final int maxCandidates;
    private final String observationLayout;

    DecisionType(int maxCandidates, String observationLayout) {
        this.maxCandidates = maxCandidates;
        this.observationLayout = observationLayout;
    }

    /**
     * Upper bound of the number of legal candidates (records are padded to it).
     */
    public int maxCandidates() {
        return maxCandidates;
    }

    /**
     * Description of the observation layout. Changing the encoder MUST change this text, it is part of the file schema hash
     * and lets a trainer refuse data produced by a different encoder.
     */
    public String observationLayout() {
        return observationLayout;
    }
}
