package com.terraforming.ares.services.ai.network2;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Why: a phase-upgrade card played when every phase already has an upgrade used to throw a NullPointerException and lose the whole game. The input
 * that replaces it must re-apply an upgrade the player already has (a no-op), because the engine has no "no upgrade" value.
 */
class PhaseUpgradeFallbackTest {

    @Test
    void reAppliesTheFirstUpgradedPhase() {
        // phase 1 has upgrade 1 -> encoded 0; re-applying 0 sets it to 1 again
        assertEquals(0, PhaseUpgradeFallback.noOp(List.of(1, 0, 0, 0, 0)));
        // phase 2 has upgrade 2 -> encoded 3; re-applying 3 sets it to 2 again
        assertEquals(3, PhaseUpgradeFallback.noOp(List.of(0, 2, 0, 0, 0)));
    }

    @Test
    void noUpgradesFallsBackToALegalValue() {
        assertEquals(0, PhaseUpgradeFallback.noOp(List.of(0, 0, 0, 0, 0)));
    }

    @Test
    void theEncodingMatchesTheEngine() {
        // UpgradePhaseHelper: phaseIndex = upgrade / 2, type = upgrade % 2 + 1. Re-applying the current type is a no-op.
        for (int phase = 0; phase < 5; phase++) {
            for (int type = 1; type <= 2; type++) {
                List<Integer> cards = new java.util.ArrayList<>(List.of(0, 0, 0, 0, 0));
                cards.set(phase, type);
                int encoded = PhaseUpgradeFallback.noOp(cards);
                assertEquals(phase, encoded / 2);
                assertEquals(type, encoded % 2 + 1);
            }
        }
    }
}
