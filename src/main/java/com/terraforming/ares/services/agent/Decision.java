package com.terraforming.ares.services.agent;

import java.util.List;

/**
 * A decision point: what the player sees and what it may choose from.
 *
 * @param observation  everything the player is allowed to know (global state vector + decision specific extras);
 *                     NEVER hidden information (opponent hand, deck order, order of the dummy phases)
 * @param candidates   the legal options, never empty
 */
public record Decision(DecisionType type, String playerUuid, float[] observation, List<Candidate> candidates) {
    public Decision {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("A decision needs at least one candidate");
        }
    }
}
