package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.CardsCollectService;
import com.terraforming.ares.services.ai.ICardValueService;
import com.terraforming.ares.services.ai.RandomBotHelper;
import lombok.RequiredArgsConstructor;
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
    private final CardsCollectService cardsCollectService;

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
            if (RandomBotHelper.isRandomBot(player)) {
                bestCard = cardsToDiscard.get(random.nextInt(cardsToDiscard.size()));
            } else if (player.isSecondBot()) {
                bestCard = cardsCollectService.getBestCard(game, player.getUuid(), cardsToDiscard);
                if (bestCard != null && Constants.LOG_NET_COMPARISON) {
                    System.out.println("Keeping ai " + cardService.getCard(bestCard).getClass().getSimpleName());
                }
            }
            if (bestCard == null) {
                bestCard = cardValueService.getBestCard(game, player, cardsToDiscard, game.getTurns());
                if (bestCard != null && Constants.LOG_NET_COMPARISON) {
                    System.out.println("Keeping statistics " + cardService.getCard(bestCard).getClass().getSimpleName());
                }
            }
            if (Constants.COLLECT_CARDS_DATASET && nextTurn.isOnlyFromSelectedCards()) {
                cardsCollectService.collectData(game, player, bestCard, game.getTurns());
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
