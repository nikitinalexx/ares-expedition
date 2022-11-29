package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.ai.CardValueService;
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
public class AiSellCardsLastRoundTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardValueService cardValueService;

    @Override
    public TurnType getType() {
        return TurnType.SELL_CARDS_LAST_ROUND;
    }

    @Override
    public void processTurn(MarsGame game, Player player) {
        List<Integer> allCards = new ArrayList<>(player.getHand().getCards());

        int cardsToSellCount = allCards.size() - 10;

        List<Integer> cardsToSell = new ArrayList<>();

        if (RandomBotHelper.isRandomBot(player)) {
            for (int i = 0; i < cardsToSellCount; i++) {
                Integer cardToSell = allCards.get(random.nextInt(allCards.size()));
                allCards.remove(cardToSell);
                cardsToSell.add(cardToSell);
            }
        } else {
            cardsToSell = cardValueService.getCardsToDiscard(game, player, allCards, cardsToSellCount);
        }

        aiTurnService.sellCardsLastRoundTurn(player, cardsToSell);
    }

}
