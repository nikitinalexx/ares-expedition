package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.NitriteReductingBacteria;
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
public class NitriteReductingBacteriaAiCardProjection<T extends Card> implements AiCardProjection<NitriteReductingBacteria> {
    private final DeepNetwork deepNetwork;
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<NitriteReductingBacteria> getType() {
        return NitriteReductingBacteria.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        if (player.getCardResourcesCount().get(NitriteReductingBacteria.class) < 3 || !terraformingService.canRevealOcean(game)) {
            player.getCardResourcesCount().put(NitriteReductingBacteria.class, player.getCardResourcesCount().get(NitriteReductingBacteria.class) + 1);
            return new MarsGameRowDifference();
        }

        player.getCardResourcesCount().put(NitriteReductingBacteria.class, player.getCardResourcesCount().get(NitriteReductingBacteria.class) + 1);
        float stateIfPutResource = deepNetwork.testState(game, player);
        player.getCardResourcesCount().put(NitriteReductingBacteria.class, player.getCardResourcesCount().get(NitriteReductingBacteria.class) - 1);


        MarsGame gameCopy = new MarsGame(game);
        Player playerCopy = new Player(player);

        playerCopy.getCardResourcesCount().put(NitriteReductingBacteria.class, player.getCardResourcesCount().get(NitriteReductingBacteria.class) - 3);
        //TODO count cards before revealOcean
        terraformingService.revealOcean(marsContextProvider.provide(gameCopy, playerCopy));

        float stateIfUseResource = deepNetwork.testState(gameCopy, playerCopy);

        if (stateIfPutResource > stateIfUseResource) {
            player.getCardResourcesCount().put(NitriteReductingBacteria.class, player.getCardResourcesCount().get(NitriteReductingBacteria.class) + 1);
        } else {
            player.getCardResourcesCount().put(NitriteReductingBacteria.class, player.getCardResourcesCount().get(NitriteReductingBacteria.class) - 3);
            terraformingService.revealOcean(marsContextProvider.provide(game, player));
        }
        return new MarsGameRowDifference();
    }
}
