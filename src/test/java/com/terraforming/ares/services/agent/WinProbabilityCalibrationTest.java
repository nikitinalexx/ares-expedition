package com.terraforming.ares.services.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Why: the win probability the UI shows is not the critics' logit - the critics are margin models, and showing {@code sigmoid(logit)} calls a
 * predicted margin of -20 a 27% chance while the measured rate there is 7% (self-play) or 4% (against NETWORK). The default pair must leave the old
 * behaviour unchanged, and the fitted pair must reproduce the curve it was fitted to.
 */
class WinProbabilityCalibrationTest {

    @Test
    void theDefaultPairIsTheRawSigmoid() {
        assertEquals(0.5f, OnePlySearch.calibratedProbability(0f, 1.0, 0.0), 1e-6);
        assertEquals(0.26894143f, OnePlySearch.calibratedProbability(-1f, 1.0, 0.0), 1e-6);
    }

    @Test
    void theMarginFitReproducesTheMeasuredCurve() {
        // logit -1 is a predicted margin of -20; the fitted pair says sigmoid(1.76 * -1 - 0.83) = ~7%
        assertEquals(0.0698f, OnePlySearch.calibratedProbability(-1f, 1.76, -0.83), 1e-3);
        // logit 0 is a predicted margin of 0; the raw sigmoid says 50%, the fit says 30.5% (the measured self-play rate in that bin)
        assertEquals(0.3036f, OnePlySearch.calibratedProbability(0f, 1.76, -0.83), 1e-3);
    }
}
