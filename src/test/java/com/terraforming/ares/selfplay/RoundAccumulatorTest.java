package com.terraforming.ares.selfplay;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Why: the round statistics are read from games finishing in parallel, and the first version of the game-length question was answered by an even split that
 * looked plausible and was not measured at all. A silent error here (min left at MAX_VALUE, a lost increment) would report a game length that no game had.
 */
class RoundAccumulatorTest {

    @Test
    void emptyReportsNoGames() {
        SelfPlayRunner.RoundStats stats = new SelfPlayRunner.RoundAccumulator().snapshot();
        assertEquals(0, stats.games());
        assertEquals(0, stats.min());
        assertEquals(0, stats.max());
        assertEquals(0.0, stats.mean());
    }

    @Test
    void minMaxAndMean() {
        SelfPlayRunner.RoundAccumulator accumulator = new SelfPlayRunner.RoundAccumulator();
        accumulator.add(25);
        accumulator.add(31);
        accumulator.add(27);
        SelfPlayRunner.RoundStats stats = accumulator.snapshot();
        assertEquals(3, stats.games());
        assertEquals(25, stats.min());
        assertEquals(31, stats.max());
        assertEquals(83 / 3.0, stats.mean(), 1e-9);
    }

    @Test
    void theSummaryJsonCarriesTheRounds() {
        // the record had the field and the hand-built summary JSON did not: this is the test that fails on that mistake
        String json = SelfPlayRunner.roundsJson(new SelfPlayRunner.RoundStats(25, 31, 82 / 3.0, 3));
        assertEquals("\"rounds\":{\"min\":25,\"max\":31,\"mean\":27.33,\"games\":3},", json);
    }

    @Test
    void parallelGamesAreAllCounted() throws Exception {        SelfPlayRunner.RoundAccumulator accumulator = new SelfPlayRunner.RoundAccumulator();
        int threads = 8;
        int perThread = 250;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        accumulator.add(30);
                    }
                    return null;
                });
            }
            start.countDown();
        } finally {
            pool.shutdown();
        }
        if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("the test pool did not finish");
        }
        SelfPlayRunner.RoundStats stats = accumulator.snapshot();
        assertEquals(threads * perThread, stats.games());
        assertEquals(30, stats.min());
        assertEquals(30, stats.max());
        assertEquals(30.0, stats.mean());
    }
}
