package com.terraforming.ares.services;

import com.terraforming.ares.model.Player;

/**
 * Created by oleksii.nikitin
 * Creation date 18.02.2023
 */
public class UpgradePhaseHelper {

    public static void upgradePhase(Player player, int upgrade) {
        //0 to 4. represents 1 to 5 phase.
        int phaseIndex = upgrade / 2;

        //0 is default phase. 1 is first upgrade, 2 is second upgrade.
        int upgradeType = upgrade % 2 + 1;

        player.updatePhaseCard(phaseIndex, upgradeType);
    }
}
