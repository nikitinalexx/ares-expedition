package com.terraforming.ares.services.agent.tree;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.agent.Decision;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

/**
 * Drives ONE simulation of the tree: at every decision of OUR seat it asks the tree which candidate to take, so the simulation
 * follows a tree line instead of the policy's line. Everything else (the opponent, the dummy) is played by the policy, and the
 * hidden information is a determinised world - the tree never branches on it.
 *
 * <p>Why a driver instead of recursion: the engine advances the game itself (turn processors), so the only way to have the search
 * choose at a node is to answer the decision when the engine asks. The driver is a thread-local the decision service consults
 * before anything else.
 *
 * <p>The first attempt at this (19/11 evening) was flat: one menu, the leaf played by the policy, 71.65%. This one spans several of
 * our decisions - that is what lets the tree compare LINES, e.g. "phase 1 now, then build the green card" against "phase 3 now".
 */
public final class TreeDriver {

    private static final ThreadLocal<TreeDriver> CURRENT = new ThreadLocal<>();

    /** One expansion on the current path: the node, the candidate index inside it, and the decision index that was returned. */
    private static final class Step {
        final TreeNode node;
        final int candidateIndex;

        Step(TreeNode node, int candidateIndex) {
            this.node = node;
            this.candidateIndex = candidateIndex;
        }
    }

    private final Function<Decision, float[]> prior;
    /** The seat the line belongs to: in self-play BOTH seats are agents, so answering the opponent would be cheating. */
    private final String seat;
    private final int maxDepth;
    private final int maxCandidates;
    private final float exploration;
    private final float parentValue;
    private final Deque<Step> path = new ArrayDeque<>();
    private final List<Step> pathList = new ArrayList<>();
    private TreeNode pendingParent;
    private int pendingIndex = -1;
    private int decisions;

    public TreeDriver(Function<Decision, float[]> prior, String seat, int maxDepth, int maxCandidates, float exploration,
                      float parentValue) {
        this.prior = prior;
        this.seat = seat;
        this.maxDepth = maxDepth;
        this.maxCandidates = maxCandidates;
        this.exploration = exploration;
        this.parentValue = parentValue;
    }

    public static TreeDriver current() {
        return CURRENT.get();
    }

    public static void set(TreeDriver driver) {
        CURRENT.set(driver);
    }

    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Called by the decision service for every decision of our seat during a simulated line. Returns the candidate index the tree
     * wants, or -1 when the tree does not want to decide this one (depth cap reached, empty menu, no legal candidate).
     */
    public int chooseIndex(Decision decision, Player player) {
        if (seat != null && player.getUuid() != null && !seat.equals(player.getUuid())) {
            return -1;   // the opponent plays by the policy; the tree grows only on our decisions
        }
        if (decisions >= maxDepth) {
            return -1;
        }
        TreeNode node = nodeFor(decision);
        if (node == null || node.size() == 0) {
            return -1;
        }
        int index = node.select(parentValue, exploration);
        if (index < 0) {
            return -1;
        }
        decisions++;
        Step step = new Step(node, index);
        path.push(step);
        pathList.add(step);
        pendingParent = node;
        pendingIndex = index;
        return node.candidateAt(index);
    }

    /** The node this decision belongs to: the root on the first call, otherwise the child of the previous choice. */
    private TreeNode nodeFor(Decision decision) {
        if (pendingParent == null) {
            return expand(decision);
        }
        TreeNode child = pendingParent.childAt(pendingIndex);
        if (child == null) {
            child = expand(decision);
            pendingParent.setChild(pendingIndex, child);
        }
        return child;
    }

    /** The node for one decision: its candidates pruned to the strongest {@code maxCandidates} by the policy's prior. */
    private TreeNode expand(Decision decision) {
        float[] probabilities = prior.apply(decision);
        int count = decision.candidates().size();
        if (probabilities == null || probabilities.length != count) {
            probabilities = new float[count];
            for (int i = 0; i < count; i++) {
                probabilities[i] = 1f / count;
            }
        }
        int keep = Math.min(maxCandidates, count);
        int[] order = new int[count];
        for (int i = 0; i < count; i++) {
            order[i] = i;
        }
        // A simple selection sort on at most ~40 candidates: no allocation-heavy sort of boxed indices on the hot path.
        for (int i = 0; i < keep; i++) {
            int best = i;
            for (int j = i + 1; j < count; j++) {
                if (probabilities[order[j]] > probabilities[order[best]]) {
                    best = j;
                }
            }
            int swap = order[i];
            order[i] = order[best];
            order[best] = swap;
        }
        int[] candidates = new int[keep];
        float[] prior = new float[keep];
        float total = 0;
        for (int i = 0; i < keep; i++) {
            candidates[i] = order[i];
            prior[i] = probabilities[order[i]];
            total += prior[i];
        }
        if (total > 0) {
            for (int i = 0; i < keep; i++) {
                prior[i] /= total;
            }
        }
        return new TreeNode(candidates, prior);
    }

    /** Adds the leaf value to every expansion on this line. */
    public void backup(float value) {
        for (Step step : pathList) {
            step.node.backup(step.candidateIndex, value);
        }
        pathList.clear();
        path.clear();
        pendingParent = null;
        pendingIndex = -1;
        decisions = 0;
    }

    /** Marks the LAST expansion as unusable (the engine rejected the applied candidate). */
    public void rejectLast() {
        if (!pathList.isEmpty()) {
            Step last = pathList.get(pathList.size() - 1);
            last.node.reject(last.candidateIndex);
        }
        pathList.clear();
        path.clear();
        pendingParent = null;
        pendingIndex = -1;
        decisions = 0;
    }

    /** The root node: the menu the whole line was built from. Null when the driver never expanded anything. */
    public TreeNode root() {
        return pathList.isEmpty() ? null : pathList.get(0).node;
    }

    public int decisionsMade() {
        return decisions;
    }
}
