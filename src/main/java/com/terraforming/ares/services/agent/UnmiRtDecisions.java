package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * UNMI corporation: spend 6 MC for +1 TR after raising TR this phase, or stop and leave the action for later / skip.
 * Offered only when the engine lists {@link TurnType#UNMI_RT} and the player can afford it.
 * <p>
 * Candidate features: one-hot kind (act / stop), MC cost scaled, whether act is affordable, stop flag.
 */
@Service
@RequiredArgsConstructor
public class UnmiRtDecisions {
    public static final int STOP = 0;
    public static final int ACT = 1;
    public static final int KINDS = 2;
    public static final int COST_MC = 6;

    public static final int MAX_CANDIDATES = KINDS;
    public static final int CANDIDATE_FEATURES = KINDS + 1 + 1 + 1;
    public static final String OBSERVATION_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[kind(" + KINDS + "), mcCost/20, canAffordAct, stop]; actId=" + ACT
            + "; stopId=" + STOP + "; unmiCost=" + COST_MC;

    private final ObservationBuilder observationBuilder;

    public static DecisionFile.Schema schema() {
        return new DecisionFile.Schema(DecisionType.UNMI_RT, ObservationBuilder.size(),
                DecisionType.UNMI_RT.maxCandidates(), CANDIDATE_FEATURES, OBSERVATION_LAYOUT);
    }

    public boolean available(Player player, List<TurnType> possibleTurns) {
        return possibleTurns.contains(TurnType.UNMI_RT) && player.getMc() >= COST_MC;
    }

    public Decision build(MarsGame game, Player player) {
        List<Candidate> candidates = new ArrayList<>(2);
        candidates.add(new Candidate(ACT, features(player, ACT)));
        candidates.add(new Candidate(STOP, features(player, STOP)));
        return new Decision(DecisionType.UNMI_RT, player.getUuid(),
                observationBuilder.build(game, player), candidates);
    }

    public float[] features(Player player, int kind) {
        float[] features = new float[CANDIDATE_FEATURES];
        features[kind] = 1f;
        int pos = KINDS;
        features[pos++] = COST_MC / 20f;
        features[pos++] = player.getMc() >= COST_MC ? 1f : 0f;
        features[pos] = kind == STOP ? 1f : 0f;
        return features;
    }
}
