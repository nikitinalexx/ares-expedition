package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.*;
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
public class SymbioticFungusAiCardProjection<T extends Card> implements AiCardProjection<SymbioticFungus> {
    private final DeepNetwork deepNetwork;

    private final List<Class<?>> cardsByPriorities = List.of(
            Tardigrades.class,
            AnaerobicMicroorganisms.class,
            DecomposingFungus.class,
            SelfReplicatingBacteria.class,
            Decomposers.class,
            GhgProductionBacteria.class,
            NitriteReductingBacteria.class,
            RegolithEaters.class
    );

    @Override
    public Class<SymbioticFungus> getType() {
        return SymbioticFungus.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        Map<Class<?>, Integer> playerResources = player.getCardResourcesCount();

        float bestState = deepNetwork.testState(game, player);
        Class<?> bestCard = null;

        for (Class<?> c : cardsByPriorities) {
            boolean result = putResourceIfPossible(playerResources, c);

            if (result) {
                float newState = deepNetwork.testState(game, player);
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
