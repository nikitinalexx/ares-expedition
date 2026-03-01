package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

public class PolicyPhaseUpgradeFeature implements PlayerPolicyFeatureBlock {

    public void encode(TableContext ctx, PolicyRecord out, int playerIndex) {
        Player player = ctx.getPlayer();

        int mask = 0;

        for (int i = 0; i < 5; i++) {
            int phaseUpgradeType = player.getPhaseCards().get(i);
            mask |= (phaseUpgradeType << (i * 2));
        }

        out.phaseUpgrades[playerIndex] = mask;
    }

}
