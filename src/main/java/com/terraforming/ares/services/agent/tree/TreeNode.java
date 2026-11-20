package com.terraforming.ares.services.agent.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * One node of the search tree: a decision of OURS with its candidates as children.
 *
 * <p>Why a separate class: the tree's bookkeeping (visits, value sums, the PUCT choice, the backup) is the part that decides which
 * line gets explored, and it can be tested without the engine. On 19/11 the first attempt was a flat allocation (one level, the
 * leaf played by the policy), which cannot compare LINES - it only spends the budget on the same plan. This is the node of a tree
 * that spans several of our decisions.
 *
 * <p>Chance (the opponent's and the dummy's moves) is not a node: it is sampled inside a simulation (a determinised world), so the
 * tree never branches on hidden information.
 */
public final class TreeNode {

    /** The candidate indices this node may expand, already pruned by the policy's prior. */
    private final int[] candidates;
    /** The prior of each candidate, renormalised over the pruned set. */
    private final float[] prior;
    private final int[] visits;
    private final float[] valueSum;
    /** Children, created lazily; {@code null} means "not expanded yet". */
    private final TreeNode[] children;

    public TreeNode(int[] candidates, float[] prior) {
        if (candidates.length != prior.length) {
            throw new IllegalArgumentException("candidates " + candidates.length + " against prior " + prior.length);
        }
        this.candidates = candidates;
        this.prior = prior;
        this.visits = new int[candidates.length];
        this.valueSum = new float[candidates.length];
        this.children = new TreeNode[candidates.length];
    }

    public int size() {
        return candidates.length;
    }

    public int candidateAt(int index) {
        return candidates[index];
    }

    public int visitsAt(int index) {
        return visits[index];
    }

    public TreeNode childAt(int index) {
        return children[index];
    }

    public void setChild(int index, TreeNode child) {
        children[index] = child;
    }

    /**
     * The candidate to expand: PUCT with the prior and the running mean. An unvisited candidate is worth the parent's value (so
     * the prior decides among equals) plus its exploration bonus, which is what makes the tree try the policy's second choice
     * at least once.
     */
    public int select(float parentValue, float exploration) {
        int best = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        int total = 0;
        for (int visit : visits) {
            total += visit;
        }
        for (int i = 0; i < visits.length; i++) {
            if (Float.isNaN(valueSum[i])) {
                continue;   // a candidate whose afterstate the engine rejects
            }
            double mean = visits[i] == 0 ? parentValue : valueSum[i] / visits[i];
            double bonus = exploration * prior[i] * Math.sqrt(total + 1) / (1 + visits[i]);
            double score = mean + bonus;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    /** Adds one sample to a candidate: its value from this player's point of view. */
    public void backup(int index, float value) {
        visits[index]++;
        valueSum[index] += value;
    }

    /** Marks a candidate as unusable for good (the engine rejected it on a copy). */
    public void reject(int index) {
        valueSum[index] = Float.NaN;
    }

    public float meanAt(int index) {
        return visits[index] == 0 ? Float.NaN : valueSum[index] / visits[index];
    }

    /** The candidate with the highest mean, i.e. what the search wants to play. */
    public int best() {
        int best = -1;
        float bestMean = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < visits.length; i++) {
            if (Float.isNaN(valueSum[i]) || visits[i] == 0) {
                continue;
            }
            float mean = valueSum[i] / visits[i];
            if (mean > bestMean) {
                bestMean = mean;
                best = i;
            }
        }
        return best;
    }

    /** Total samples under this node (its own visits, since each candidate sample is one visit). */
    public int totalVisits() {
        int total = 0;
        for (int visit : visits) {
            total += visit;
        }
        return total;
    }

    /** The children that exist, for debugging and for the tests. */
    public List<TreeNode> expandedChildren() {
        List<TreeNode> found = new ArrayList<>();
        for (TreeNode child : children) {
            if (child != null) {
                found.add(child);
            }
        }
        return found;
    }
}
