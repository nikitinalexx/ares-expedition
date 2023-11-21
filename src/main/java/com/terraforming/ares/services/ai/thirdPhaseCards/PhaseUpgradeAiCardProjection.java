package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import com.terraforming.ares.services.ai.dto.PhaseUpgradeWithChance;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class PhaseUpgradeAiCardProjection<T extends Card> implements AiCardProjection<T> {
    protected AiDiscoveryDecisionService aiDiscoveryDecisionService;

    protected PhaseUpgradeAiCardProjection(AiDiscoveryDecisionService aiDiscoveryDecisionService) {
        this.aiDiscoveryDecisionService = aiDiscoveryDecisionService;
    }


    public MarsGameRowDifference produceDifferenceForPhaseUpgrade(MarsGame game, Player player) {
        List<PhaseUpgradeWithChance> sortedUpgrades = aiDiscoveryDecisionService
                .chooseBestUpdateForEachPhase(game, player).stream()
                .sorted(Comparator.comparing(PhaseUpgradeWithChance::getChance).reversed())
                .collect(Collectors.toList());

        for (PhaseUpgradeWithChance sortedUpgrade : sortedUpgrades) {
            int phase = sortedUpgrade.getPhase();
            if (player.getPhaseCards().get(phase) == sortedUpgrade.getUpgrade()) {
                continue;
            } else {
                player.getPhaseCards().set(phase, sortedUpgrade.getUpgrade());
            }
        }

        return new MarsGameRowDifference();
    }

}
