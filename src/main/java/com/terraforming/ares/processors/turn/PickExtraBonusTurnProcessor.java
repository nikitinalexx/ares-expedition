package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.PickExtraBonusSecondPhase;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class PickExtraBonusTurnProcessor implements TurnProcessor<PickExtraBonusSecondPhase> {
    private final CardService cardService;

    @Override
    public TurnType getType() {
        return TurnType.PICK_EXTRA_BONUS_SECOND_PHASE;
    }

    @Override
    public TurnResponse processTurn(PickExtraBonusSecondPhase turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        if (player.hasPhaseUpgrade(Constants.PHASE_2_NO_UPGRADE)) {
            List<Integer> cards = cardService.dealCards(game, 1);

            for (Integer card : cards) {
                player.getHand().addCard(card);
            }
        }

        if (player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_MC)) {
            player.setMc(player.getMc() + 6);
        }

        player.setGotBonusInSecondPhase(true);
        player.setActionsInSecondPhase(player.getActionsInSecondPhase() - 1);

        return null;
    }
}
