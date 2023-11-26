package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.DeepNetwork;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DecomposingFungusAiCardProjection<T extends Card> implements AiCardProjection<DecomposingFungus> {
    private final DeepNetwork deepNetwork;

    private final List<Class<?>> cardsByPriorities = List.of(
            Birds.class,
            Fish.class,
            Livestock.class,
            ArclightCorporation.class,
            BuffedArclightCorporation.class,
            EcologicalZone.class,
            Herbivores.class,
            SmallAnimals.class,
            Tardigrades.class,
            FilterFeeders.class,
            AnaerobicMicroorganisms.class,
            DecomposingFungus.class,
            SelfReplicatingBacteria.class,
            Decomposers.class,
            GhgProductionBacteria.class,
            NitriteReductingBacteria.class,
            RegolithEaters.class
    );

    @Override
    public Class<DecomposingFungus> getType() {
        return DecomposingFungus.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        Map<Class<?>, Integer> playerResources = player.getCardResourcesCount();

        float bestState = deepNetwork.testState(game, player, network);
        Class<?> bestCard = null;

        for (Class<?> c : cardsByPriorities) {
            boolean result = removeResourceIfPossible(playerResources, c);

            if (result) {
                player.setPlants(player.getPlants() + 3);
                float newState = deepNetwork.testState(game, player, network);
                if (newState > bestState) {
                    bestState = newState;
                    bestCard = c;
                }
                addResource(playerResources, c);
                player.setPlants(player.getPlants() - 3);
            }
        }

        if (bestCard != null) {
            removeResourceIfPossible(playerResources, bestCard);
            player.setPlants(player.getPlants() + 3);
        }

        return new MarsGameRowDifference();
    }

    private boolean removeResourceIfPossible(Map<Class<?>, Integer> playerResources, Class<?> card) {
        if (playerResources.getOrDefault(card, 0) > 0) {
            playerResources.put(card, playerResources.get(card) - 1);
            return true;
        }
        return false;
    }

    private void addResource(Map<Class<?>, Integer> playerResources, Class<?> card) {
        playerResources.put(card, playerResources.get(card) + 1);
    }

}
