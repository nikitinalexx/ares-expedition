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
        // Two pools, one per CARD KIND: the project cards (the opponent's hand + the projects deck) and the corporations
        // (the opponent's offers + the corporations deck). Cards of one kind are exchangeable among the slots of that kind;
        // cards of different kinds are not, and mixing them produced illegal positions - a project card sitting in the
        // "offered corporations" slot, which any rollout that then asks that seat for a corporation dies on
        // ("Corporation <a project> applies an on-built effect to itself, but the legal inputs are not described").
        List<List<Integer>> projects = new ArrayList<>();
        projects.add(other.getHand().getCards());
        projects.add(game.getProjectsDeck().getCards());
        List<List<Integer>> corporations = new ArrayList<>();
        corporations.add(other.getCorporations().getCards());
        if (game.getCorporationsDeck() != null) {
            corporations.add(game.getCorporationsDeck().getCards());
        }
        shuffleWithinKind(projects, seed);
        shuffleWithinKind(corporations, seed + 1);
    }

    /** Shuffles the cards of one kind across the slots of that kind, keeping every slot's size. */
    private static void shuffleWithinKind(List<List<Integer>> slots, long seed) {
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
