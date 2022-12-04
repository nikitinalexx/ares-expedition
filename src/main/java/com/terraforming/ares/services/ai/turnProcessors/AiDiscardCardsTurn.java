package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.ai.ICardValueService;
import com.terraforming.ares.services.ai.RandomBotHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiDiscardCardsTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final ICardValueService cardValueService;

    @Override
    public TurnType getType() {
        return TurnType.DISCARD_CARDS;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        DiscardCardsTurn nextTurn = (DiscardCardsTurn) player.getNextTurn();

        List<Integer> cardsToDiscard;
        if (nextTurn.isOnlyFromSelectedCards()) {
            cardsToDiscard = new ArrayList<>(nextTurn.getCards());
        } else {
            cardsToDiscard = new ArrayList<>(player.getHand().getCards());
        }

        int cardsToKeep = cardsToDiscard.size() - nextTurn.getSize();

        for (int i = 0; i < cardsToKeep; i++) {
            //keep best card
            Integer bestCard;
            if (RandomBotHelper.isRandomBot(player)) {
                bestCard = cardsToDiscard.get(random.nextInt(cardsToDiscard.size()));
            } else {
                bestCard = cardValueService.getBestCard(game, player, cardsToDiscard, game.getTurns());
            }
            cardsToDiscard.remove(bestCard);
        }

        aiTurnService.discardCards(
                game,
                player,
                new DiscardCardsTurn(player.getUuid(), cardsToDiscard, cardsToDiscard.size(), false, false),
                cardsToDiscard
        );
        return true;
    }

}
