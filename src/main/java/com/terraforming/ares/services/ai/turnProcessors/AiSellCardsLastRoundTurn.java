package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ai.AiCardsChoice;
import com.terraforming.ares.model.ai.AiExperimentalTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.ai.AiPickCardProjectionService;
import com.terraforming.ares.services.ai.ICardValueService;
import com.terraforming.ares.services.ai.network2.Network2DiscardCardsProcessor;
import com.terraforming.ares.services.policyai.PolicyCollectService;
import com.terraforming.ares.services.policyai.service.PolicyDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiSellCardsLastRoundTurn implements AiTurnProcessor {
    private final AiTurnService aiTurnService;
    private final ICardValueService cardValueService;
    private final AiPickCardProjectionService aiPickCardProjectionService;
    private final Network2DiscardCardsProcessor network2DiscardCardsProcessor;
    private final PolicyCollectService policyCollectService;
    private final PolicyDecisionService policyDecisionService;

    @Override
    public TurnType getType() {
        return TurnType.SELL_CARDS_LAST_ROUND;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        List<Integer> allCards = new ArrayList<>(player.getHand().getCards());

        int cardsToSellCount = allCards.size() - 10;

        List<Integer> cardsToSell = new ArrayList<>();

        if (player.getDifficulty().EXPERIMENTAL_TURN == AiExperimentalTurn.POLICY) {
            cardsToSell = policyDecisionService.sellCardsAfterGenerationEnd(game, player, cardsToSellCount);
        } else if (player.getDifficulty().EXPERIMENTAL_TURN == AiExperimentalTurn.EXPERIMENT) {
            List<Integer> cardsToKeep = network2DiscardCardsProcessor.getBestCards(game, player, allCards, 10);
            cardsToSell.addAll(allCards);
            cardsToSell.removeAll(cardsToKeep);
        } else if (player.getDifficulty().CARDS_PICK == AiCardsChoice.NETWORK_PROJECTION) {
            cardsToSell = aiPickCardProjectionService.getCardsToSell(game, player, allCards, cardsToSellCount);
        } else {
            for (int i = 0; i < cardsToSellCount; i++) {
                Integer cardToSell;
                switch (player.getDifficulty().CARDS_PICK) {
                    case RANDOM:
                        cardToSell = allCards.get(ThreadLocalRandom.current().nextInt(allCards.size()));
                        break;
                    case FILE_VALUE:
                        cardToSell = cardValueService.getWorstCard(game, player, allCards, game.getTurns()).getCardId();
                        break;
                    default:
                        throw new IllegalStateException("Computer unable to sell a Card");
                }
                allCards.remove(cardToSell);
                cardsToSell.add(cardToSell);
            }
        }

        aiTurnService.sellCardsLastRoundTurn(player, cardsToSell);

        return true;
    }

}
