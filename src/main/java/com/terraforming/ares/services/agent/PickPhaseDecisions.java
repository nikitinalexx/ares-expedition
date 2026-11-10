package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builds the PICK_PHASE decision: legal phases (everything except the phase picked in the previous round),
 * the observation (state vector + extras) and candidate features.
 * <p>
 * Extras layout ({@link #EXTRAS_SIZE}): round/40, my previous phase one-hot(5), opponent previous phase one-hot(5),
 * dummy pool membership(5), dummy pool size/5.
 * The dummy pool is described as a SET: its order is hidden information (the next dummy phase is already shuffled).
 */
@Service
@RequiredArgsConstructor
public class PickPhaseDecisions {
    public static final int PHASES = 5;
    public static final int EXTRAS_SIZE = 1 + PHASES + PHASES + PHASES + 1;
    public static final int CANDIDATE_FEATURES = PHASES + 1;
    public static final String OBSERVATION_LAYOUT = "AdvancedAiDataCollectionService.collectData(table 417 + hand 481, normalized by AiConstants.NORMALIZATION_VECTOR)"
            + " + extras[round/40, myPreviousPhase(5), opponentPreviousPhase(5), dummyPoolMembership(5), dummyPoolSize/5]"
            + "; candidate=[phaseOneHot(5), phaseUpgradedForMe]";

    private final AdvancedAiDataCollectionService dataCollectionService;

    public Decision build(MarsGame game, Player player) {
        Player opponent = opponentOf(game, player);

        float[] state = dataCollectionService.collectData(game, player.getUuid());
        float[] observation = new float[state.length + EXTRAS_SIZE];
        System.arraycopy(state, 0, observation, 0, state.length);

        int pos = state.length;
        observation[pos++] = game.getTurns() / 40f;
        pos = oneHot(observation, pos, player.getPreviousChosenPhase());
        pos = oneHot(observation, pos, opponent.getPreviousChosenPhase());
        Set<Integer> pool = dummyPhasePool(game);
        for (int phase = 1; phase <= PHASES; phase++) {
            observation[pos++] = pool.contains(phase) ? 1f : 0f;
        }
        observation[pos] = pool.isEmpty() ? 0f : pool.size() / (float) PHASES;

        List<Candidate> candidates = new ArrayList<>();
        for (int phase : legalPhases(player)) {
            float[] features = new float[CANDIDATE_FEATURES];
            features[phase - 1] = 1f;
            features[PHASES] = player.isPhaseUpgraded(phase) ? 1f : 0f;
            candidates.add(new Candidate(phase, features));
        }
        return new Decision(DecisionType.PICK_PHASE, player.getUuid(), observation, candidates);
    }

    /**
     * Rule: any phase 1..5 except the one chosen in the previous round.
     */
    public static List<Integer> legalPhases(Player player) {
        List<Integer> phases = new ArrayList<>();
        for (int phase = 1; phase <= PHASES; phase++) {
            if (player.getPreviousChosenPhase() == null || player.getPreviousChosenPhase() != phase) {
                phases.add(phase);
            }
        }
        return phases;
    }

    /**
     * The phases the dummy may still draw from the pool in the next draw. An empty pool is refilled with all five phases
     * before the next draw, so for the player that is the same as a full pool.
     */
    public static Set<Integer> dummyPhasePool(MarsGame game) {
        Set<Integer> pool = new TreeSet<>();
        if (game.isDummyHandMode()) {
            if (game.getDummyHand() == null || game.getDummyHand().isEmpty()) {
                for (int phase = 1; phase <= PHASES; phase++) {
                    pool.add(phase);
                }
            } else {
                pool.addAll(game.getDummyHand());
            }
        }
        return pool;
    }

    private static int oneHot(float[] target, int pos, Integer phase) {
        if (phase != null && phase >= 1 && phase <= PHASES) {
            target[pos + phase - 1] = 1f;
        }
        return pos + PHASES;
    }

    private static Player opponentOf(MarsGame game, Player player) {
        return game.getPlayerUuidToPlayer().values().stream()
                .filter(p -> !p.getUuid().equals(player.getUuid()))
                .findFirst().orElseThrow();
    }
}
