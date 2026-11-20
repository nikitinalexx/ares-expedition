package com.terraforming.ares.services.agent;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;

import java.util.List;

/**
 * A generator instrument, not a rule of the player: pick the first offered card of a given colour.
 *
 * <p>Why it exists (owner, 20/11): the NETWORK gap is measured on events - NETWORK plays 8.6 red cards per game against our 4.2 - and the only way
 * to ask "can this player learn that plan" is to make it practise it. The colour is a property of the card data, not a strategy of ours, and the
 * instrument is the same kind as the existing plan nudges: it only switches which exploration profile produced a game.
 *
 * <p>The colour is read from the candidate's identity bit ({@link CardFeatures}), so nothing here needs a service, and the class-to-colour mapping is
 * the same package-based one {@link PlayableCards} uses. No hash, one bit per card - the owner's rule.
 */
public final class ColourNudge {

    private ColourNudge() {
    }

    /**
     * @return the index of the first candidate of {@code wanted} colour, or -1 when this menu has none (or the candidates are not cards, e.g. a
     *         stop-only menu).
     */
    public static int indexOfColour(List<Candidate> candidates, CardColor wanted) {
        for (int i = 0; i < candidates.size(); i++) {
            Class<? extends Card> card = cardOf(candidates.get(i));
            if (card != null && colourOf(card) == wanted) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return the index of the first candidate NOT of {@code wanted} colour, or -1 when every card in the menu has it (or the menu has no cards).
     *         Used where the action REMOVES a card - mulligan, discard, sell - so that "practice red" means keeping the red cards.
     */
    public static int indexOfNotColour(List<Candidate> candidates, CardColor wanted) {
        for (int i = 0; i < candidates.size(); i++) {
            Class<? extends Card> card = cardOf(candidates.get(i));
            if (card != null && colourOf(card) != null && colourOf(card) != wanted) {
                return i;
            }
        }
        return -1;
    }

    /** The card class a candidate's identity bit stands for, or null when the candidate is the stop flag / not a card. */
    static Class<? extends Card> cardOf(Candidate candidate) {
        float[] features = candidate.features();
        if (features == null || features.length <= CardFeatures.IDENTITY) {
            return null;
        }
        for (int bit = 0; bit < CardFeatures.IDENTITY; bit++) {
            if (features[bit] > 0.5f) {
                return bit < PlayableCards.ALL.size() ? PlayableCards.ALL.get(bit) : null;
            }
        }
        return null;
    }

    /** The colour of a card class, from the package it lives in (the same rule {@link PlayableCards} uses for BLUE). */
    static CardColor colourOf(Class<? extends Card> card) {
        String pkg = card.getPackageName();
        if (pkg.endsWith(".blue")) {
            return CardColor.BLUE;
        }
        if (pkg.endsWith(".green")) {
            return CardColor.GREEN;
        }
        if (pkg.endsWith(".red")) {
            return CardColor.RED;
        }
        return null;
    }
}
