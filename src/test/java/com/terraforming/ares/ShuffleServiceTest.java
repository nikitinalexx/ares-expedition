package com.terraforming.ares;

import com.terraforming.ares.services.ShuffleService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A shuffle must make every permutation equally likely. Swapping each position with a random index of the WHOLE list
 * (instead of one of the remaining positions) is a classic mistake that favours some permutations.
 */
class ShuffleServiceTest {
    @Test
    void allPermutationsOfThreeElementsAreEquallyLikely() {
        ShuffleService shuffleService = new ShuffleService();
        int trials = 120_000;
        Map<List<Integer>, Integer> counts = new HashMap<>();
        for (int i = 0; i < trials; i++) {
            List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
            shuffleService.shuffle(list);
            counts.merge(list, 1, Integer::sum);
        }

        assertTrue(counts.size() == 6, "Not all permutations were produced: " + counts);
        double expected = trials / 6.0;
        for (Map.Entry<List<Integer>, Integer> e : counts.entrySet()) {
            // 5 sigma for a binomial with p = 1/6 is about 3.4% of the expectation
            assertTrue(Math.abs(e.getValue() - expected) < 0.05 * expected,
                    "Biased shuffle: " + counts + " (expected about " + (int) expected + " each)");
        }
    }

    @Test
    void everyElementCanEndUpInEveryPositionEquallyOften() {
        ShuffleService shuffleService = new ShuffleService();
        int size = 10;
        int trials = 100_000;
        int[][] positions = new int[size][size];
        for (int t = 0; t < trials; t++) {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(i);
            }
            shuffleService.shuffle(list);
            for (int pos = 0; pos < size; pos++) {
                positions[list.get(pos)][pos]++;
            }
        }
        double expected = trials / (double) size;
        for (int element = 0; element < size; element++) {
            for (int pos = 0; pos < size; pos++) {
                assertTrue(Math.abs(positions[element][pos] - expected) < 0.06 * expected,
                        "Element " + element + " lands on position " + pos + " " + positions[element][pos] + " times, expected about " + (int) expected);
            }
        }
    }
}
