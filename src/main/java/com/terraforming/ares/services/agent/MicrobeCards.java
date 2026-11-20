package com.terraforming.ares.services.agent;

import com.terraforming.ares.model.Card;

import java.util.Set;

/**
 * The cards that make up the microbe economy: the ones that hold or produce microbes, the ones that consume them, and the red one-shots that hand a
 * microbe over.
 *
 * <p>Why an explicit set and not the MICROBE tag (owner, 20/11): the tag says almost nothing - many cards carry it without producing or storing a
 * single microbe, and the cards that matter are defined by the RESOURCE. So the set is:
 * <ul>
 *   <li>holders/producers (the owner's list): AnaerobicMicroorganisms, ConservedBiome, Decomposers, DecomposingFungus, ExtremeColdFungus, FilterFeeders,
 *       GhgProductionBacteria, NitriteReductingBacteria, RegolithEaters, SelfReplicatingBacteria, SymbioticFungus, Tardigrades, ViralEnhancers;</li>
 *   <li>consumers: Worms, BacterialAggregates;</li>
 *   <li>red one-shots with CardAction.ADD_MICROBE (found in the code): ControlledBloom, Cyanobacteria, ImportedNitrogen, ImportedNutrients;</li>
 *   <li>the buffed variants of the same cards, because the expansion replaces them by name.</li>
 * </ul>
 *
 * <p>This is card data, not a strategy: the set says which cards interact with the microbe resource, and nothing about when to play them.
 */
public final class MicrobeCards {

    private static final Set<String> NAMES = Set.of(
            // holders and producers
            "AnaerobicMicroorganisms",
            "ConservedBiome",
            "Decomposers",
            "DecomposingFungus",
            "ExtremeColdFungus",
            "FilterFeeders",
            "GhgProductionBacteria",
            "NitriteReductingBacteria",
            "RegolithEaters",
            "SelfReplicatingBacteria",
            "SymbioticFungus",
            "Tardigrades",
            "ViralEnhancers",
            // consumers
            "Worms",
            "BacterialAggregates",
            // red one-shots (CardAction.ADD_MICROBE)
            "ControlledBloom",
            "Cyanobacteria",
            "ImportedNitrogen",
            "ImportedNutrients",
            // buffed variants used when the buffed expansion is on
            "BuffedExtremeColdFungus",
            "BuffedGhgProductionBacteria",
            "BuffedRegolithEaters"
    );

    private MicrobeCards() {
    }

    /** True when the card belongs to the microbe economy, decided by its class name. */
    public static boolean isMicrobe(Card card) {
        return card != null && isMicrobeName(card.getClass().getSimpleName());
    }

    static boolean isMicrobeName(String simpleName) {
        return simpleName != null && NAMES.contains(simpleName);
    }

    /** For the instruments that need the size of the family. */
    public static int size() {
        return NAMES.size();
    }
}
