package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiPickCardProjectionService;
import com.terraforming.ares.services.ai.ICardValueService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
    private final CardService cardService;
    private final AiPickCardProjectionService aiPickCardProjectionService;

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
            Integer bestCard = null;

            switch (player.getDifficulty().CARDS_PICK) {
                case FILE_VALUE:
                    bestCard = cardValueService.getBestCard(game, player, cardsToDiscard, game.getTurns());
                    if (bestCard != null && Constants.LOG_NET_COMPARISON) {
                        System.out.println("Keeping statistics " + cardService.getCard(bestCard).getClass().getSimpleName());
                    }
                    break;
                case NETWORK_PROJECTION:
                    bestCard = aiPickCardProjectionService.getBestCard(game, player, cardsToDiscard);
                    break;

                case RANDOM:
                    bestCard = cardsToDiscard.get(random.nextInt(cardsToDiscard.size()));
                    break;
            }

            cardsToDiscard.remove(bestCard);
        }
        if (Constants.LOG_NET_COMPARISON) {
            System.out.println("Discarding " + cardsToDiscard.stream().map(cardService::getCard).map(card -> card.getClass().getSimpleName()).collect(Collectors.joining(",")));
            System.out.println();
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
