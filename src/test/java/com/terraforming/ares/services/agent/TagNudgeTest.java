package com.terraforming.ares.services.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Why this test exists: the tag nudge produces the corpus for the microbe experiment, and its two directions are easy to invert - the build menu must
 * take the card WITH the tag, the discard menu must give up a card WITHOUT it. An inverted nudge would produce a corpus of anti-microbe play and the
 * experiment would "show" that microbes hurt.
 */
class TagNudgeTest {

    private static Candidate card(int id) {
        return new Candidate(id, new float[CardFeatures.LENGTH]);
    }

    private static Candidate stop() {
        return new Candidate(CardFeatures.STOP_ID, new float[CardFeatures.LENGTH]);
    }

    private static final IntPredicateHolder MICROBE = new IntPredicateHolder(Set.of(101, 102, 103));

    /** Small holder so the predicate reads as "does this id carry the tag" in the assertions. */
    private record IntPredicateHolder(Set<Integer> ids) implements java.util.function.IntPredicate {
        @Override
        public boolean test(int id) {
            return ids.contains(id);
        }
    }

    @Test
    void buildMenuTakesTheTaggedCard() {
        List<Candidate> menu = List.of(card(1), card(101), card(2));
        assertEquals(1, TagNudge.index(menu, MICROBE, false));
    }

    @Test
    void buildMenuWithoutTheTagReturnsMinusOne() {
        assertEquals(-1, TagNudge.index(List.of(card(1), card(2)), MICROBE, false));
    }

    @Test
    void keepMenuGivesUpTheUntaggedCard() {
        List<Candidate> menu = List.of(card(101), card(1), card(2));
        assertEquals(1, TagNudge.index(menu, MICROBE, true));
    }

    @Test
    void keepMenuWithOnlyTaggedCardsReturnsMinusOne() {
        assertEquals(-1, TagNudge.index(List.of(card(101), card(102)), MICROBE, true));
    }

    @Test
    void theStopCandidateIsNeverChosen() {
        List<Candidate> menu = List.of(stop(), card(101));
        assertEquals(1, TagNudge.index(menu, MICROBE, false));
        assertEquals(-1, TagNudge.index(List.of(stop(), card(101)), MICROBE, true));
    }
}
