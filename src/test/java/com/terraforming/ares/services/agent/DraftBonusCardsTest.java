package com.terraforming.ares.services.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why this test exists: the owner's correction was that the draft bonus comes from four specific cards, not from the phase-5 upgrade. This test pins
 * that family, so a later edit cannot quietly replace it with the upgrade (which is a different mechanic: it raises the picker's own payout once).
 */
class DraftBonusCardsTest {

    @Test
    void theFourDraftCardsAreIn() {
        for (String card : new String[]{"ExtendedResources", "Interns", "UnitedPlanetaryAlliance", "BacterialAggregates"}) {
            assertTrue(DraftBonusCards.isDraftBonusName(card), card + " must be part of the draft-bonus family");
        }
    }

    @Test
    void otherCardsAreOut() {
        assertFalse(DraftBonusCards.isDraftBonusName("Fish"));
        assertFalse(DraftBonusCards.isDraftBonusName("Worms"));
        assertFalse(DraftBonusCards.isDraftBonusName(null));
    }

    @Test
    void theFamilyHasFourCards() {
        assertEquals(4, DraftBonusCards.size());
    }
}
