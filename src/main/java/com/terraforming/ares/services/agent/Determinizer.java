package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Hidden-information determinization: shuffle the cards the moving player cannot see (the opponent's hand and
 * corporations, both decks) over all slots that hold them. The observable position is untouched, so a rollout started
 * from a determinized copy is a legal sample of "what the hidden part might be".
 * <p>
 * Two uses: the one-ply search samples it to make candidate comparisons fair (common random numbers per decision), and the
 * rollout audits sample it to get independent noise for out-of-sample evaluation - a candidate selected on one
 * determinization is evaluated on another, which is what removes the winner's curse of picking the best of several noisy
 * rollouts.
 */
public final class Determinizer {

    private Determinizer() {
    }

    public static void shuffle(MarsGame game, String myUuid, long seed) {
        Player me = game.getPlayerByUuid(myUuid);
        if (me == null) {
            return;
        }
        Player other = null;
        for (Player candidate : game.getPlayerUuidToPlayer().values()) {
            if (!candidate.getUuid().equals(me.getUuid())) {
                other = candidate;
                break;
            }
        }
        if (other == null) {
            return;
        }
        List<List<Integer>> slots = new ArrayList<>();
        slots.add(other.getHand().getCards());
        slots.add(other.getCorporations().getCards());
        slots.add(game.getProjectsDeck().getCards());
        if (game.getCorporationsDeck() != null) {
            slots.add(game.getCorporationsDeck().getCards());
        }
        int total = 0;
        for (List<Integer> slot : slots) {
            total += slot.size();
        }
        if (total < 2) {
            return;
        }
        List<Integer> pool = new ArrayList<>(total);
        for (List<Integer> slot : slots) {
            pool.addAll(slot);
        }
        Collections.shuffle(pool, new Random(seed));
        int offset = 0;
        for (List<Integer> slot : slots) {
            int size = slot.size();
            slot.clear();
            slot.addAll(pool.subList(offset, offset + size));
            offset += size;
        }
    }
}
