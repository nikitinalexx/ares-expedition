package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.RegolithEaters;
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
public class RegolithEatersAiCardProjection<T extends Card> implements AiCardProjection<RegolithEaters> {
    private final DeepNetwork deepNetwork;
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<RegolithEaters> getType() {
        return RegolithEaters.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        if (player.getCardResourcesCount().get(RegolithEaters.class) < 2 || !terraformingService.canIncreaseOxygen(game)) {
            player.getCardResourcesCount().put(RegolithEaters.class, player.getCardResourcesCount().get(RegolithEaters.class) + 1);
            return new MarsGameRowDifference();
        }

        player.getCardResourcesCount().put(RegolithEaters.class, player.getCardResourcesCount().get(RegolithEaters.class) + 1);
        float stateIfPutResource = deepNetwork.testState(game, player);
        player.getCardResourcesCount().put(RegolithEaters.class, player.getCardResourcesCount().get(RegolithEaters.class) - 1);


        MarsGame gameCopy = new MarsGame(game);
        Player playerCopy = new Player(player);

        playerCopy.getCardResourcesCount().put(RegolithEaters.class, player.getCardResourcesCount().get(RegolithEaters.class) - 2);
        terraformingService.raiseOxygen(marsContextProvider.provide(gameCopy, playerCopy));

        float stateIfUseResource = deepNetwork.testState(gameCopy, playerCopy);

        if (stateIfPutResource > stateIfUseResource) {
            player.getCardResourcesCount().put(RegolithEaters.class, player.getCardResourcesCount().get(RegolithEaters.class) + 1);
        } else {
            player.getCardResourcesCount().put(RegolithEaters.class, player.getCardResourcesCount().get(RegolithEaters.class) - 2);
            terraformingService.raiseOxygen(marsContextProvider.provide(game, player));
        }
        return new MarsGameRowDifference();
    }
}
