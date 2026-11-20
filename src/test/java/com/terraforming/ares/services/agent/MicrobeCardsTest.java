package com.terraforming.ares.services.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why this test exists: the microbe set decides which cards the practice arm will chase, and the owner's correction was precisely that the MICROBE TAG is
 * not the right definition. A test that pins the family to the resource cards (holders, consumers, the red one-shots) keeps a future edit from drifting
 * back to the tag.
 */
class MicrobeCardsTest {

    @Test
    void holdersAndProducersAreIn() {
        for (String card : new String[]{"AnaerobicMicroorganisms", "ConservedBiome", "Decomposers", "DecomposingFungus",
                "ExtremeColdFungus", "FilterFeeders", "GhgProductionBacteria", "NitriteReductingBacteria",
                "RegolithEaters", "SelfReplicatingBacteria", "SymbioticFungus", "Tardigrades", "ViralEnhancers"}) {
            assertTrue(MicrobeCards.isMicrobeName(card), card + " must be part of the microbe economy");
        }
    }

    @Test
    void consumersAndRedOneShotsAreIn() {
        for (String card : new String[]{"Worms", "BacterialAggregates", "ControlledBloom", "Cyanobacteria",
                "ImportedNitrogen", "ImportedNutrients"}) {
            assertTrue(MicrobeCards.isMicrobeName(card), card + " must be part of the microbe economy");
        }
    }

    @Test
    void buffedVariantsAreIn() {
        assertTrue(MicrobeCards.isMicrobeName("BuffedRegolithEaters"));
        assertTrue(MicrobeCards.isMicrobeName("BuffedGhgProductionBacteria"));
        assertTrue(MicrobeCards.isMicrobeName("BuffedExtremeColdFungus"));
    }

    @Test
    void cardsThatMerelyMentionMicrobesAreOut() {
        // SeedBank, FibrousCompositeMaterial and HydroponicGardens mention microbes in their text but do not produce or hold the resource
        assertFalse(MicrobeCards.isMicrobeName("SeedBank"));
        assertFalse(MicrobeCards.isMicrobeName("FibrousCompositeMaterial"));
        assertFalse(MicrobeCards.isMicrobeName("Fish"));
        assertFalse(MicrobeCards.isMicrobeName(null));
    }

    @Test
    void theFamilyIsTheOwnerListPlusTheRedOneShots() {
        assertEquals(22, MicrobeCards.size());
    }
}
