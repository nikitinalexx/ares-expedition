package com.terraforming.ares.services.agent;

import com.terraforming.ares.cards.blue.GreenHouses;
import com.terraforming.ares.cards.green.Blueprints;
import com.terraforming.ares.cards.red.AssortedEnterprises;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Why this test exists: the colour nudge is a generator instrument that will produce a corpus, and a silent error (asking for red and getting
 * green, or matching the stop candidate) would make the "red practice" experiment measure something else entirely - the class of mistake that has
 * cost this project the most (docs/METHOD.md).
 *
 * <p>Note the names: the colour comes from the CARD PACKAGE, not from the class name. `GreenHouses` is a blue card and `Blueprints` is green, which
 * is exactly the kind of trap this test exists for.
 */
class ColourNudgeTest {

    private static Candidate candidateOf(Class<? extends Card> card) {
        float[] features = new float[CardFeatures.LENGTH];
        features[PlayableCards.ALL.indexOf(card)] = 1f;
        return new Candidate(card.getSimpleName().hashCode() & 0x7FFF, features);
    }

    private static Candidate stop() {
        float[] features = new float[CardFeatures.LENGTH];
        features[CardFeatures.IDENTITY] = 1f;
        return new Candidate(CardFeatures.STOP_ID, features);
    }

    @Test
    void findsTheColourItWasAskedFor() {
        List<Candidate> menu = List.of(candidateOf(GreenHouses.class), candidateOf(Blueprints.class),
                candidateOf(AssortedEnterprises.class));
        assertEquals(2, ColourNudge.indexOfColour(menu, CardColor.RED));
        assertEquals(1, ColourNudge.indexOfColour(menu, CardColor.GREEN));
        assertEquals(0, ColourNudge.indexOfColour(menu, CardColor.BLUE));
    }

    @Test
    void returnsMinusOneWhenTheColourIsAbsent() {
        List<Candidate> menu = List.of(candidateOf(Blueprints.class), stop());
        assertEquals(-1, ColourNudge.indexOfColour(menu, CardColor.RED));
    }

    @Test
    void skipsTheStopCandidate() {
        List<Candidate> menu = List.of(stop(), candidateOf(AssortedEnterprises.class));
        assertEquals(1, ColourNudge.indexOfColour(menu, CardColor.RED));
    }

    @Test
    void stopOnlyMenuHasNoColour() {
        List<Candidate> menu = new ArrayList<>();
        menu.add(stop());
        assertEquals(-1, ColourNudge.indexOfColour(menu, CardColor.RED));
    }

    @Test
    void colourComesFromTheCardPackageNotTheName() {
        assertEquals(CardColor.BLUE, ColourNudge.colourOf(GreenHouses.class));
        assertEquals(CardColor.GREEN, ColourNudge.colourOf(Blueprints.class));
        assertEquals(CardColor.RED, ColourNudge.colourOf(AssortedEnterprises.class));
        assertNull(ColourNudge.colourOf(Card.class));
    }
}
