package com.terraforming.ares.services.agent;

import java.util.List;
import java.util.Map;

/**
 * One legal way to take the corporation: the card and the input its on-built effect requires.
 * {@link #candidate()} is what the network scores; {@code id} of that candidate is the corporation card id
 * (the same card can appear several times, once per input — the chosen INDEX selects the input).
 */
public record CorporationChoice(int corporationId, Map<Integer, List<Integer>> input, Candidate candidate) {
}
