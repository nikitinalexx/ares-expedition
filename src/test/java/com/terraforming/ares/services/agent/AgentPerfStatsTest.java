package com.terraforming.ares.services.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentPerfStatsTest {
    @Test
    void tracksMeansAndCpuShare() {
        AgentPerfStats.reset();
        AgentPerfStats.recordObservation(2_000_000);
        AgentPerfStats.recordObservation(4_000_000);
        AgentPerfStats.recordNeural(1_000_000, 4);
        AgentPerfStats.recordNeural(3_000_000, 6);
        AgentPerfStats.recordFallback();

        AgentPerfStats.Snapshot snap = AgentPerfStats.snapshot();
        assertEquals(2, snap.observationCalls());
        assertEquals(3000.0, snap.observationUsMean(), 0.1);
        assertEquals(2, snap.neuralCalls());
        assertEquals(2000.0, snap.neuralUsMean(), 0.1);
        assertEquals(5.0, snap.neuralCandidatesMean(), 0.01);
        assertEquals(1, snap.fallbackCalls());
        // 4ms of neural work over 1s * 8 threads => 0.0005 of the CPU budget
        assertEquals(0.0005, snap.neuralCpuShare(1.0, 8), 1e-6);
        assertTrue(snap.progressLine(1.0, 8, 12.0).contains("games/s"));
        assertTrue(snap.toMap(1.0, 8, 12.0).get("hint").toString().contains("GPU unlikely"));
    }
}
