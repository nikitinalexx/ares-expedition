package com.terraforming.ares.services.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Why: the win probability the UI shows is not the critics' logit - the critics are margin models, and showing {@code sigmoid(logit)} calls a
 * predicted margin of -20 a 27% chance while the measured rate there is 7%. The DEFAULT pair must already be the fitted one (this project's critics are
 * margin models); a probability-trained critic can opt back into the raw sigmoid with the properties.
 */
class WinProbabilityCalibrationTest {

    @Test
    void theDefaultIsTheFittedMarginCalibration() {
        // logit 0 is a predicted margin of 0: the raw sigmoid says 50%, the measured self-play rate in that bin is 30.5%
        assertEquals(0.3036f, OnePlySearch.defaultWinProbability(0f), 1e-3);
        // logit -1 is a predicted margin of -20: the measured rate there is ~7%, not the 26.9% the raw sigmoid shows
        assertEquals(0.0698f, OnePlySearch.defaultWinProbability(-1f), 1e-3);
    }

    @Test
    void theRawSigmoidIsStillAvailable() {
        assertEquals(0.5f, OnePlySearch.calibratedProbability(0f, 1.0, 0.0), 1e-6);
        assertEquals(0.26894143f, OnePlySearch.calibratedProbability(-1f, 1.0, 0.0), 1e-6);
    }
}
