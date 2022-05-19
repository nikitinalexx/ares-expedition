package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.DiscardDraftedCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class DiscardDraftedCardsProcessor implements TurnProcessor<DiscardDraftedCardsTurn> {

    @Override
    public TurnResponse processTurn(DiscardDraftedCardsTurn turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());

        player.getHand().removeCards(turn.getCards());

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.DISCARD_DRAFTED_CARDS;
    }
}
