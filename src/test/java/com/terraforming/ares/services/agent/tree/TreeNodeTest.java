package com.terraforming.ares.services.agent.tree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tree's bookkeeping: the prior decides among equals, a better mean wins, a rejected candidate is out, and the backup is one
 * sample per visit.
 *
 * Why these cases: the flat first attempt on 19/11 spent the budget without ever comparing lines, which is why it scored 71.65%
 * instead of beating 73.17%. This node is the piece that must behave correctly for a tree that spans several of our decisions, and
 * it can be checked without the engine.
 */
class TreeNodeTest {

    @Test
    void thePriorDecidesAmongUnvisitedCandidates() {
        TreeNode node = new TreeNode(new int[]{10, 11, 12}, new float[]{0.1f, 0.7f, 0.2f});
        assertEquals(1, node.select(0f, 0.5f), "the policy's favourite is expanded first");
    }

    @Test
    void aCandidateWithABetterMeanWins() {
        TreeNode node = new TreeNode(new int[]{1, 2}, new float[]{0.4f, 0.6f});
        node.backup(0, 2f);
        node.backup(0, 2f);
        node.backup(0, 2f);
        node.backup(1, 0.1f);
        assertEquals(0, node.select(0f, 0.5f), "three samples at +2 beat one at +0.1 despite the prior");
        assertEquals(0, node.best());
    }

    @Test
    void anUnvisitedCandidateIsWorthTheParentValueNotZero() {
        // A brand-new candidate must not look losing just because it has no samples: its mean falls back to the parent's value, so
        // it beats a candidate that has been sampled and found bad. With a mean of zero it would never be tried at all.
        TreeNode node = new TreeNode(new int[]{1, 2}, new float[]{0.9f, 0.1f});
        node.backup(0, 0f);   // the visited one sits at a losing value
        assertEquals(1, node.select(3f, 0.1f), "the unvisited one inherits the parent value and beats a sampled bad candidate");
    }

    @Test
    void aRejectedCandidateIsNeverSelectedAgain() {
        TreeNode node = new TreeNode(new int[]{1, 2}, new float[]{0.9f, 0.1f});
        node.reject(0);
        node.backup(1, 1f);
        assertEquals(1, node.select(0f, 0.5f));
        assertEquals(1, node.best());
    }

    @Test
    void theBackupCountsOneSamplePerVisit() {
        TreeNode node = new TreeNode(new int[]{1, 2}, new float[]{0.5f, 0.5f});
        node.backup(0, 1f);
        node.backup(0, 3f);
        assertEquals(2, node.visitsAt(0));
        assertEquals(2f, node.meanAt(0), 1e-6);
        assertEquals(2, node.totalVisits());
    }

    @Test
    void childrenAreCreatedLazily() {
        TreeNode node = new TreeNode(new int[]{1}, new float[]{1f});
        assertNull(node.childAt(0), "an unexpanded child is null");
        TreeNode child = new TreeNode(new int[]{5}, new float[]{1f});
        node.setChild(0, child);
        assertEquals(child, node.childAt(0));
        assertEquals(1, node.expandedChildren().size());
    }

    @Test
    void anEmptyMenuIsHarmless() {
        TreeNode node = new TreeNode(new int[0], new float[0]);
        assertEquals(-1, node.select(0f, 0.5f));
        assertEquals(-1, node.best());
        assertEquals(0, node.totalVisits());
        assertTrue(node.expandedChildren().isEmpty());
        assertNotEquals(1, node.size());
    }
}
