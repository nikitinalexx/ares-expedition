package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import com.terraforming.ares.services.ai.AiCollectIncomePhaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiCollectIncomeTurn implements AiTurnProcessor {
    private final AiTurnService aiTurnService;
    private final AiCollectIncomePhaseService  aiCollectIncomePhaseService;

    @Override
    public TurnType getType() {
        return TurnType.COLLECT_INCOME;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {

        Integer doubleIncomeCard = null;

        if (player.getChosenPhase() == 4 && player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_DOUBLE_PRODUCE)) {
            doubleIncomeCard = aiCollectIncomePhaseService.getDoubleIncomeCard(game, player);
        }

        aiTurnService.collectIncomeTurn(player, doubleIncomeCard);

        return true;
    }

}
