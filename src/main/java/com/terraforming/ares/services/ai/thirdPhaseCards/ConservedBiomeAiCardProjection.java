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
public class ConservedBiomeAiCardProjection<T extends Card> implements AiCardProjection<ConservedBiome> {
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
    public Class<ConservedBiome> getType() {
        return ConservedBiome.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        Map<Class<?>, Integer> playerResources = player.getCardResourcesCount();

        float bestState = deepNetwork.testState(game, player, network);
        Class<?> bestCard = null;

        for (Class<?> c : cardsByPriorities) {
            boolean result = putResourceIfPossible(playerResources, c);

            if (result) {
                float newState = deepNetwork.testState(game, player, network);
                if (newState > bestState) {
                    bestState = newState;
                    bestCard = c;
                }
                removeResource(playerResources, c);
            }
        }

        if (bestCard != null) {
            putResourceIfPossible(playerResources, bestCard);
        }

        return new MarsGameRowDifference();
    }

    private boolean putResourceIfPossible(Map<Class<?>, Integer> playerResources, Class<?> card) {
        if (playerResources.containsKey(card)) {
            playerResources.put(card, playerResources.get(card) + 1);
            return true;
        }
        return false;
    }

    private void removeResource(Map<Class<?>, Integer> playerResources, Class<?> card) {
        playerResources.put(card, playerResources.get(card) - 1);
    }

}
