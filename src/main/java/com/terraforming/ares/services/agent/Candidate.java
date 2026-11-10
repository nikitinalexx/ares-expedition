package com.terraforming.ares.services.agent;

/**
 * One legal option of a decision.
 *
 * @param id       what the engine needs to execute it (for PICK_PHASE: the phase number 1..5)
 * @param features description of the option itself, fed to the network together with the observation
 */
public record Candidate(int id, float[] features) {
}
