package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.FibrousCompositeMaterial;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import com.terraforming.ares.services.ai.DeepNetwork;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FibrousCompositeMaterialAiCardProjection<T extends Card> extends PhaseUpgradeAiCardProjection<FibrousCompositeMaterial> {
    private final DeepNetwork deepNetwork;

    public FibrousCompositeMaterialAiCardProjection(AiDiscoveryDecisionService aiDiscoveryDecisionService, DeepNetwork deepNetwork) {
        super(aiDiscoveryDecisionService);
        this.deepNetwork = deepNetwork;
    }

    @Override
    public Class<FibrousCompositeMaterial> getType() {
        return FibrousCompositeMaterial.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        if (player.getCardResourcesCount().get(FibrousCompositeMaterial.class) < 3) {
            player.getCardResourcesCount().put(FibrousCompositeMaterial.class, player.getCardResourcesCount().get(FibrousCompositeMaterial.class) + 1);
            return new MarsGameRowDifference();
        }

        player.getCardResourcesCount().put(FibrousCompositeMaterial.class, player.getCardResourcesCount().get(FibrousCompositeMaterial.class) + 1);
        float stateIfPutResource = deepNetwork.testState(game, player, network);
        player.getCardResourcesCount().put(FibrousCompositeMaterial.class, player.getCardResourcesCount().get(FibrousCompositeMaterial.class) - 4);

        List<Integer> originalPhaseCards = new ArrayList<>(player.getPhaseCards());

        produceDifferenceForPhaseUpgrade(game, player, network);

        float stateIfUseResource = deepNetwork.testState(game, player, network);

        if (stateIfPutResource > stateIfUseResource) {
            player.setPhaseCards(originalPhaseCards);
            player.getCardResourcesCount().put(FibrousCompositeMaterial.class, player.getCardResourcesCount().get(FibrousCompositeMaterial.class) + 4);
        }

        return new MarsGameRowDifference();
    }

}
