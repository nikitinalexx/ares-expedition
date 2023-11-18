package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.dataset.CardsAiService;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiPickCardProjectionService;
import com.terraforming.ares.services.ai.ICardValueService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final CardsAiService cardsAiService;
    private final AiPickCardProjectionService aiPickCardProjectionService;

    public static final Map<AiDiscardCardsKey, List<AiDiscardCardsValue>> AI_DISCARD_CARDS_STATS = new ConcurrentHashMap<>();

    @Value
    private static class AiDiscardCardsKey {
        int initialScore;
        int turn;
    }

    @Value
    private static class AiDiscardCardsValue {
        double initialScore;
        double finalScore;
    }

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

        double initialScore = 0;
        if (Constants.COLLECT_FIFTH_PHASE_STATISTICS && nextTurn.isOnlyFromSelectedCards()) {
            LinkedList<Integer> handBeforeFifthPhase = new LinkedList<>(player.getHand().getCards());
            handBeforeFifthPhase.removeAll(cardsToDiscard);

            MarsGame gameCopy = new MarsGame(game);
            gameCopy.getPlayerByUuid(player.getUuid()).setHand(Deck.builder().cards(handBeforeFifthPhase).build());

            initialScore = handBeforeFifthPhase.stream().map(
                    card -> cardsAiService.getCardChance(gameCopy, player.getUuid(), card)
            ).sorted(Collections.reverseOrder()).limit(10).mapToDouble(Double::doubleValue).sum() * 100;
        }


        int cardsToKeep = cardsToDiscard.size() - nextTurn.getSize();

        for (int i = 0; i < cardsToKeep; i++) {
            //keep best card
            Integer bestCard = null;

            switch (player.isFirstBot() ? Constants.CARDS_PICK_PLAYER_1 : Constants.CARDS_PICK_PLAYER_2) {
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
                case NETWORK:
                    bestCard = cardsAiService.getBestCard(game, player.getUuid(), cardsToDiscard);
                    if (Constants.LOG_NET_COMPARISON) {
                        System.out.println("Keeping ai " + cardService.getCard(bestCard).getClass().getSimpleName());
                    }
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

        if (Constants.COLLECT_FIFTH_PHASE_STATISTICS && nextTurn.isOnlyFromSelectedCards()) {
            LinkedList<Integer> handAfterFifthPhase = new LinkedList<>(player.getHand().getCards());
            handAfterFifthPhase.removeAll(cardsToDiscard);

            MarsGame gameCopy = new MarsGame(game);
            gameCopy.getPlayerByUuid(player.getUuid()).setHand(Deck.builder().cards(handAfterFifthPhase).build());

            double finalScore = handAfterFifthPhase.stream().map(
                    card -> cardsAiService.getCardChance(gameCopy, player.getUuid(), card)
            ).sorted(Collections.reverseOrder()).limit(10).mapToDouble(Double::doubleValue).sum() * 100;

            if (finalScore < initialScore) {
                finalScore = initialScore;
            }

            AiDiscardCardsValue e = new AiDiscardCardsValue(initialScore, finalScore);

            AI_DISCARD_CARDS_STATS.compute(new AiDiscardCardsKey((int) initialScore, game.getTurns()), (key, value) -> {
                if (value == null) {
                    value = new ArrayList<>();
                }
                value.add(e);
                return value;
            });
        }


        return true;
    }

}
