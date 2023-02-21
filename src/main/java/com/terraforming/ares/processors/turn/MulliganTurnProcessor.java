package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.MulliganTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class MulliganTurnProcessor implements TurnProcessor<MulliganTurn> {
    private final CardService cardService;


    @Override
    public TurnResponse processTurn(MulliganTurn turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());

        player.setMulligan(false);

        List<Integer> cards = turn.getCards();

        player.getHand().removeCards(cards);
        player.getHand().addCards(cardService.dealCards(game, cards.size()));

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.MULLIGAN;
    }
}
