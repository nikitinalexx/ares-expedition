package com.terraforming.ares.services.ai;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.11.2022
 */
@Service
public class CardValueService {
    private final DeepNetwork deepNetwork;

    public CardValueService(DeepNetwork deepNetwork) {
        this.deepNetwork = deepNetwork;
    }

    public List<Integer> getCardsToDiscard(MarsGame game, Player player, List<Integer> cardsToDiscardFrom, int count) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        List<Integer> cardsToDiscard = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int worstCardIndex = -1;
            float worstChance = 1.0f;

            for (int j = 0; j < cardsToDiscardFrom.size(); j++) {
                player.getHand().removeCard(cardsToDiscardFrom.get(j));
                float currentChance = deepNetwork.testState(game, player);

                if (currentChance <= worstChance) {
                    worstCardIndex = j;
                    worstChance = currentChance;
                }

                player.getHand().addCard(cardsToDiscardFrom.get(j));
            }

            player.getHand().removeCard(cardsToDiscardFrom.get(worstCardIndex));
            cardsToDiscard.add(cardsToDiscardFrom.get(worstCardIndex));
            cardsToDiscardFrom.remove(worstCardIndex);
        }

        return cardsToDiscard;
    }

}
