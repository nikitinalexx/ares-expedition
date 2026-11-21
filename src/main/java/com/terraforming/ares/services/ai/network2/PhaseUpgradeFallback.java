package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.model.Player;

/**
 * When every phase already has an upgrade, a phase-upgrade card still has to be played for its tags and its other effects. The engine has no "no
 * upgrade" input, but re-applying the upgrade a phase already has is a no-op ({@code UpgradePhaseHelper.upgradePhase} sets the phase card to the same
 * type), so the card can be played without changing anything. This replaces an NPE that made the whole game count as failed (owner, 21/11).
 */
public final class PhaseUpgradeFallback {

    private PhaseUpgradeFallback() {
    }

    /** An input value that leaves the current upgrades as they are: the phase's own upgrade, encoded as {@code phaseIndex * 2 + (type - 1)}. */
    public static int noOp(Player player) {
        return noOp(player.getPhaseCards());
    }

    static int noOp(java.util.List<Integer> phaseCards) {
        for (int i = 0; i < phaseCards.size(); i++) {
            int value = phaseCards.get(i);
            if (value != 0) {
                return i * 2 + (value == 1 ? 0 : 1);
            }
        }
        // No upgrades at all cannot happen on this path (the caller only comes here when the optimizer found none because all are taken),
        // but 0 is always a legal input value.
        return 0;
    }
}
