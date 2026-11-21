package com.terraforming.ares;

import com.terraforming.ares.controllers.SimulationCompareController;
import com.terraforming.ares.model.PlayerDifficulty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A benchmark game can also be training data: {@code -Dares.compare.record=DIR} writes the agent's decisions of every
 * compared game, labelled by the outcome, through the same {@code DecisionLogs} the play mode uses.
 * <p>
 * Why this exists: the critic has to be trained on the distribution it is deployed on - our player against the opponent
 * we measure against - and the benchmark already plays exactly those games. Recording during it costs no extra
 * generation. The failure mode this test pins is silent: without {@code stopRecording} the chunks stay `.tmp` files that
 * the packer cannot read, and the corpus is simply empty after an hour of measurement.
 */
class CompareRecordingTest extends EngineTestBase {

    @Autowired
    SimulationCompareController controller;

    @Test
    void a_compare_writes_sealed_chunks_when_recording_is_on() throws Exception {
        Path dir = Files.createTempDirectory("compare-record");
        System.setProperty("ares.compare.record", dir.toString());
        try {
            controller.compare(2, PlayerDifficulty.AGENT, PlayerDifficulty.RANDOM, 1);
        } finally {
            System.clearProperty("ares.compare.record");
        }

        long sealed;
        try (Stream<Path> files = Files.list(dir)) {
            sealed = files.filter(p -> p.getFileName().toString().endsWith(".ares")).count();
        }
        assertTrue(sealed > 0, "no *.ares chunk in " + dir + ": the recording was started but never sealed, so the corpus "
                + "of the measurement is lost");
    }
}
