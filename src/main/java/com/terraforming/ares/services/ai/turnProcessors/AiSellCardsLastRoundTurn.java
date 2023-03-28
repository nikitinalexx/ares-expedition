package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ai.AiTurnChoice;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.dataset.CardsAiService;
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
            if (player.isFirstBot() && Constants.FIRST_DISCARD_OR_SELL_CARDS_TURN == AiTurnChoice.RANDOM || player.isSecondBot() && Constants.SECOND_DISCARD_OR_SELL_CARDS_TURN == AiTurnChoice.RANDOM) {
                cardToSell = allCards.get(random.nextInt(allCards.size()));
            } else if (player.isFirstBot() && Constants.FIRST_DISCARD_OR_SELL_CARDS_TURN == AiTurnChoice.FILE_VALUE || player.isSecondBot() && Constants.SECOND_DISCARD_OR_SELL_CARDS_TURN == AiTurnChoice.FILE_VALUE){
                cardToSell = cardValueService.getWorstCard(game, player, allCards, game.getTurns()).getCardId();
            } else if (player.isFirstBot() && Constants.FIRST_DISCARD_OR_SELL_CARDS_TURN == AiTurnChoice.NETWORK || player.isSecondBot() && Constants.SECOND_DISCARD_OR_SELL_CARDS_TURN == AiTurnChoice.NETWORK){
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
