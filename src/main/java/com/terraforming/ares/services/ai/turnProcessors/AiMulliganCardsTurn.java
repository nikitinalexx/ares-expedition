package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.ai.helpers.AiCardActionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiMulliganCardsTurn implements AiTurnProcessor {
    private final AiTurnService aiTurnService;
    private final AiCardActionHelper aiCardActionHelper;

    @Override
    public TurnType getType() {
        return TurnType.MULLIGAN;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        List<Integer> cardsToDiscardSmart = aiCardActionHelper.getCardsToDiscardSmart(game, player, player.getHand().getCards().size());
        aiTurnService.mulliganCards(game, player, cardsToDiscardSmart);
        return true;
    }

}
