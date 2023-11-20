package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.GreenHouses;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.DeepNetwork;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GreenHousesAiCardProjection<T extends Card> implements AiCardProjection<GreenHouses> {
    private final DeepNetwork deepNetwork;

    @Override
    public Class<GreenHouses> getType() {
        return GreenHouses.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        if (player.getHeat() <= 0) {
            return new MarsGameRowDifference();
        }

        float bestState = deepNetwork.testState(game, player);
        int count = 0;
        for (int i = 1; i <= 4 && i <= player.getHeat(); i++) {
            player.setHeat(player.getHeat() - i);
            player.setPlants(player.getPlants() + i);

            float currentState = deepNetwork.testState(game, player);
            if (currentState > bestState) {
                bestState = currentState;
                count = i;
            }

            player.setHeat(player.getHeat() + i);
            player.setPlants(player.getPlants() - i);

        }

        if (count > 0) {
            player.setHeat(player.getHeat() - count);
            player.setPlants(player.getPlants() + count);
        }

        return new MarsGameRowDifference();
    }
}
