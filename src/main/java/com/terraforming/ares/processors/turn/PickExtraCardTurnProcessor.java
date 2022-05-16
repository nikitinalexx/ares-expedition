package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.PickExtraCardTurn;
import com.terraforming.ares.model.turn.TurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class PickExtraCardTurnProcessor implements TurnProcessor<PickExtraCardTurn> {

    @Override
    public TurnType getType() {
        return TurnType.PICK_EXTRA_CARD;
    }

    @Override
    public TurnResponse processTurn(PickExtraCardTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        List<Integer> cards = game.dealCards(1);

        for (Integer card : cards) {
            player.getHand().addCard(card);
        }
        player.setPickedCardInSecondPhase(true);
        player.setActionsInSecondPhase(player.getActionsInSecondPhase() - 1);

        return null;
    }
}
