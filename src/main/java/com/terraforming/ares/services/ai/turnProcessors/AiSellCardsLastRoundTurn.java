package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ai.AiCardsChoice;
import com.terraforming.ares.model.ai.AiExperimentalTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiPickCardProjectionService;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.ICardValueService;
import com.terraforming.ares.services.ai.dto.CardValueResponse;
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
public class AiSellCardsLastRoundTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final ICardValueService cardValueService;
    private final CardService cardService;
    private final AiPickCardProjectionService aiPickCardProjectionService;
    private final DeepNetwork deepNetwork;

    @Override
    public TurnType getType() {
        return TurnType.SELL_CARDS_LAST_ROUND;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        List<Integer> allCards = new ArrayList<>(player.getHand().getCards());

        int cardsToSellCount = allCards.size() - 10;

        List<Integer> cardsToSell = new ArrayList<>();

        if (player.getDifficulty().CARDS_PICK == AiCardsChoice.NETWORK_PROJECTION) {
            cardsToSell = aiPickCardProjectionService.getWorstCards(game, player, allCards, cardsToSellCount);
        } else {
            for (int i = 0; i < cardsToSellCount; i++) {
                Integer cardToSell;
                switch (player.getDifficulty().CARDS_PICK) {
                    case RANDOM:
                        cardToSell = allCards.get(random.nextInt(allCards.size()));
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
