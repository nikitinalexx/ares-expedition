package com.terraforming.ares.services.agent;

import java.util.List;
import java.util.function.IntPredicate;

/**
 * A generator instrument, not a rule of the player: prefer a card carrying a tag (microbes, animals, energy, ...) at the build and draft menus, and
 * keep such cards at the menus that REMOVE a card.
 *
 * <p>Why it exists (owner, 20/11): microbes are too few to be a strategy of their own, but they may be a useful supplement - "show it that sometimes
 * this complements a good result". The tag is card data, not an opinion about strategy, and the instrument is the same kind as the colour nudge: it
 * changes which games are produced, nothing else.
 *
 * <p>The decision is a pure function of the candidate ids and a predicate, so it is testable without the engine (see TagNudgeTest).
 */
public final class TagNudge {

    private TagNudge() {
    }

    /**
     * @param hasTag   whether the card id carries the tag; called once per card candidate
     * @param keep     true for the menus that remove a card (mulligan, discard, sell): then the first candidate WITHOUT the tag is taken; false for
     *                 the menus that add a card (build, draft): then the first candidate WITH it is taken
     * @return the index of the chosen candidate, or -1 when the menu has nothing that fits
     */
    public static int index(List<Candidate> candidates, IntPredicate hasTag, boolean keep) {
        for (int i = 0; i < candidates.size(); i++) {
            int id = candidates.get(i).id();
            if (id <= 0 || id == CardFeatures.STOP_ID) {
                continue;   // the stop candidate is not a card, and it is never the practice target
            }
            boolean carries = hasTag.test(id);
            if (carries != keep) {
                return i;
            }
        }
        return -1;
    }
}
