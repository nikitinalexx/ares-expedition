package com.terraforming.ares.services.agent;

/**
 * Chooses among the legal candidates of a decision. Implementations: uniform random (baseline / tests),
 * neural network (later).
 */
public interface DecisionAgent {
    /**
     * @return index into {@link Decision#candidates()}
     */
    int choose(Decision decision);
}
