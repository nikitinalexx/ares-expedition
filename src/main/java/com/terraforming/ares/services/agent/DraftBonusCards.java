package com.terraforming.ares.services.agent;

import com.terraforming.ares.model.Card;

import java.util.Set;

/**
 * The cards that make the draft (phase 5) better: they raise how many cards the player SEES or KEEPS during the research phase.
 *
 * <p>Why an explicit set (owner, 21/11): the draft bonus is not the phase-5 upgrade - it is these cards, and the engine really implements them
 * (`DraftCardsService.countExtraCardsToSee/ToTake` reads {@code SpecialEffect.INTERNS} and the microbe count on BacterialAggregates). So a player that
 * wants to live off the draft needs to build them:
 * <ul>
 *   <li><b>ExtendedResources</b> - keep one additional card;</li>
 *   <li><b>Interns</b> - draw two additional cards;</li>
 *   <li><b>UnitedPlanetaryAlliance</b> - draw one and keep one additional card;</li>
 *   <li><b>BacterialAggregates</b> - draw an additional card per microbe on it (max 5), the bridge between the microbe economy and the draft.</li>
 * </ul>
 * The Tharsis corporation carries the same +1/+1 and is handled by the corporation choice, not here.
 *
 * <p>Card data, not a strategy: the set says which cards touch the draft bonus, nothing about when to play them.
 */
public final class DraftBonusCards {

    private static final Set<String> NAMES = Set.of(
            "ExtendedResources",
            "Interns",
            "UnitedPlanetaryAlliance",
            "BacterialAggregates"
    );

    private DraftBonusCards() {
    }

    public static boolean isDraftBonus(Card card) {
        return card != null && isDraftBonusName(card.getClass().getSimpleName());
    }

    static boolean isDraftBonusName(String simpleName) {
        return simpleName != null && NAMES.contains(simpleName);
    }

    public static int size() {
        return NAMES.size();
    }
}
