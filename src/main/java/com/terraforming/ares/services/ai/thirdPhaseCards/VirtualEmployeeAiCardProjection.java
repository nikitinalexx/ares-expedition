package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.VirtualEmployeeDevelopment;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import org.springframework.stereotype.Component;

@Component
public class VirtualEmployeeAiCardProjection<T extends Card> extends PhaseUpgradeAiCardProjection<VirtualEmployeeDevelopment> {

    public VirtualEmployeeAiCardProjection(AiDiscoveryDecisionService aiDiscoveryDecisionService) {
        super(aiDiscoveryDecisionService);
    }

    @Override
    public Class<VirtualEmployeeDevelopment> getType() {
        return VirtualEmployeeDevelopment.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        return produceDifferenceForPhaseUpgrade(game, player, network);
    }

}
