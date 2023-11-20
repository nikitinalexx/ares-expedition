package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.GhgProductionBacteria;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import com.terraforming.ares.services.ai.DeepNetwork;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GhgProductionBacteriaAiCardProjection<T extends Card> implements AiCardProjection<GhgProductionBacteria> {
    private final DeepNetwork deepNetwork;
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<GhgProductionBacteria> getType() {
        return GhgProductionBacteria.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        if (player.getCardResourcesCount().get(GhgProductionBacteria.class) < 2 || !terraformingService.canIncreaseTemperature(game)) {
            player.getCardResourcesCount().put(GhgProductionBacteria.class, player.getCardResourcesCount().get(GhgProductionBacteria.class) + 1);
            return new MarsGameRowDifference();
        }

        player.getCardResourcesCount().put(GhgProductionBacteria.class, player.getCardResourcesCount().get(GhgProductionBacteria.class) + 1);
        float stateIfPutResource = deepNetwork.testState(game, player);
        player.getCardResourcesCount().put(GhgProductionBacteria.class, player.getCardResourcesCount().get(GhgProductionBacteria.class) - 1);


        MarsGame gameCopy = new MarsGame(game);
        Player playerCopy = new Player(player);

        playerCopy.getCardResourcesCount().put(GhgProductionBacteria.class, player.getCardResourcesCount().get(GhgProductionBacteria.class) - 2);
        terraformingService.increaseTemperature(marsContextProvider.provide(gameCopy, playerCopy));

        float stateIfUseResource = deepNetwork.testState(gameCopy, playerCopy);

        if (stateIfPutResource > stateIfUseResource) {
            player.getCardResourcesCount().put(GhgProductionBacteria.class, player.getCardResourcesCount().get(GhgProductionBacteria.class) + 1);
        } else {
            player.getCardResourcesCount().put(GhgProductionBacteria.class, player.getCardResourcesCount().get(GhgProductionBacteria.class) - 2);
            terraformingService.increaseTemperature(marsContextProvider.provide(game, player));
        }
        return new MarsGameRowDifference();
    }
}
