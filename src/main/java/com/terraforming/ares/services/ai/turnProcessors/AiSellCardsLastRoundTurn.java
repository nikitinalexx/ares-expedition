package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ai.AiCardsChoice;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.dataset.CardsAiService;
import com.terraforming.ares.services.ai.AiPickCardProjectionService;
import com.terraforming.ares.services.ai.ICardValueService;
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
    private final CardsAiService cardsAiService;
    private final CardService cardService;
    private final AiPickCardProjectionService aiPickCardProjectionService;

    @Override
    public TurnType getType() {
        return TurnType.SELL_CARDS_LAST_ROUND;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        List<Integer> allCards = new ArrayList<>(player.getHand().getCards());

        int cardsToSellCount = allCards.size() - 10;

        List<Integer> cardsToSell = new ArrayList<>();

        for (int i = 0; i < cardsToSellCount; i++) {
            Integer cardToSell;
            if (player.isFirstBot() && Constants.CARDS_PICK_PLAYER_1 == AiCardsChoice.RANDOM || player.isSecondBot() && Constants.CARDS_PICK_PLAYER_2 == AiCardsChoice.RANDOM) {
                cardToSell = allCards.get(random.nextInt(allCards.size()));
            } else if (player.isFirstBot() && Constants.CARDS_PICK_PLAYER_1 == AiCardsChoice.FILE_VALUE || player.isSecondBot() && Constants.CARDS_PICK_PLAYER_2 == AiCardsChoice.FILE_VALUE){
                if (Constants.FIRST_PLAYER_NETWORK_PROJECTION_SELL_CARDS && player.isFirstBot()) {
                    cardToSell = aiPickCardProjectionService.getWorstCard(game, player, allCards, game.getTurns()).getCardId();
                } else {
                    cardToSell = cardValueService.getWorstCard(game, player, allCards, game.getTurns()).getCardId();
                }
            } else if (player.isFirstBot() && Constants.CARDS_PICK_PLAYER_1 == AiCardsChoice.NETWORK || player.isSecondBot() && Constants.CARDS_PICK_PLAYER_2 == AiCardsChoice.NETWORK){
                cardToSell = cardsAiService.getWorstCard(game, player.getUuid(), allCards, false);
                if (Constants.LOG_NET_COMPARISON) {
                    System.out.println("Selling ai " + cardService.getCard(cardToSell).getClass().getSimpleName());
                }
            } else {
                throw new IllegalStateException("Computer unable to sell a Card");
            }
            allCards.remove(cardToSell);
            cardsToSell.add(cardToSell);
        }

        aiTurnService.sellCardsLastRoundTurn(player, cardsToSell);

        return true;
    }

}
