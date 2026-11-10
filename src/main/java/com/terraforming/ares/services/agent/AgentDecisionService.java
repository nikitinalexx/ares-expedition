package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Entry point used by the AI turn processors of the AGENT difficulty.
 */
@Service
@RequiredArgsConstructor
public class AgentDecisionService {
    private final PickPhaseDecisions pickPhaseDecisions;
    private final DecisionAgent agent;
    private final NeuralPolicy neuralPolicy;
    private final DecisionLogs decisionLogs;

    public int pickPhase(MarsGame game, Player player) {
        Decision decision = pickPhaseDecisions.build(game, player);
        int index = neuralPolicy.hasModel() ? neuralPolicy.choose(decision) : agent.choose(decision);
        if (index < 0 || index >= decision.candidates().size()) {
            throw new IllegalStateException("Agent chose " + index + " out of " + decision.candidates().size() + " candidates");
        }
        decisionLogs.record(game, player, decision, index);
        return decision.candidates().get(index).id();
    }
}
