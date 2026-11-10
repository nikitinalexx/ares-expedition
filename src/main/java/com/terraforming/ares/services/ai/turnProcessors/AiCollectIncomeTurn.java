package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.agent.AgentDecisionService;
import com.terraforming.ares.services.ai.AiCollectIncomePhaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiCollectIncomeTurn implements AiTurnProcessor {
    private final AiTurnService aiTurnService;
    private final AiCollectIncomePhaseService aiCollectIncomePhaseService;
    private final AgentDecisionService agentDecisionService;

    @Override
    public TurnType getType() {
        return TurnType.COLLECT_INCOME;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        if (player.getDifficulty().isAgent()) {
            Integer doubleCard = agentDecisionService.collectIncome(game, player).orElse(null);
            agentDecisionService.playCollectIncome(player, doubleCard);
            return true;
        }

        Integer doubleIncomeCard = null;
        if (player.getChosenPhase() == 4 && player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_DOUBLE_PRODUCE)) {
            doubleIncomeCard = aiCollectIncomePhaseService.getDoubleIncomeCard(game, player);
        }
        aiTurnService.collectIncomeTurn(player, doubleIncomeCard);
        return true;
    }

}
