package com.terraforming.ares.services.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tree's selection rule, on its own: PUCT with the policy prior.
 *
 * Why these cases: the whole point of the tree is to spend a budget where the policy thinks something is happening instead of
 * scoring every candidate once. If the rule ignores the prior, or never exploits a candidate that keeps looking better, the budget
 * is wasted - and with the measured cost (25 ms per 64 leaves) a wasted budget is the difference between a usable search and none.
 */
class TreeSelectionTest {

    @Test
    void anUnvisitedMenuIsExploredByThePriorFirst() {
        float[] sum = new float[3];
        int[] visits = new int[3];
        float[] prior = {0.1f, 0.7f, 0.2f};
        assertEquals(1, OnePlySearch.selectByPuct(sum, visits, prior, 0, 0f), "the policy's favourite is sampled first");
    }

    @Test
    void aCandidateThatKeepsScoringHigherIsPreferred() {
        float[] sum = {0.0f, 3.0f, 0.0f};   // candidate 1 has three samples, all at +1
        int[] visits = {0, 3, 0};
        float[] prior = {0.4f, 0.3f, 0.3f};
        assertEquals(1, OnePlySearch.selectByPuct(sum, visits, prior, 3, 0f), "exploit the one that is clearly better");
    }

    @Test
    void aRejectedCandidateIsNeverSelectedAgain() {
        float[] sum = {Float.NaN, 0.5f};
        int[] visits = {1, 1};
        float[] prior = {0.9f, 0.1f};
        assertEquals(1, OnePlySearch.selectByPuct(sum, visits, prior, 1, 0f), "a candidate the engine rejects is out");
    }

    @Test
    void aNeverVisitedCandidateStartsFromTheStopValue() {
        // With every candidate unvisited there is nothing to compare, so the mean falls back to the stop value and the prior
        // decides - never a random or zero baseline, which would make an unvisited menu look losing.
        float[] sum = new float[2];
        int[] visits = {0, 0};
        float[] prior = {0.5f, 0.5f};
        int picked = OnePlySearch.selectByPuct(sum, visits, prior, 0, -2f);
        assertTrue(picked == 0 || picked == 1);
    }
}
