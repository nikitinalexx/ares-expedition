package com.terraforming.ares.selfplay;

import com.terraforming.ares.EngineTestBase;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.agent.DecisionFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfPlayRunnerTest extends EngineTestBase {
    @TempDir
    Path out;

    @Autowired
    SelfPlayRunner runner;

    private static SelfPlayRunner.Options options(Path out, int games, int chunk, long maxSeconds, Path stopFile) {
        return new SelfPlayRunner.Options(out, games, 4,
                List.of(new SelfPlayRunner.Matchup(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT, 1)),
                chunk, maxSeconds, stopFile, 0.05, 0);
    }

    private List<Path> chunks() throws IOException {
        try (var files = Files.list(out)) {
            return files.filter(f -> f.getFileName().toString().matches("PICK_PHASE\\.\\d+\\.ares")).sorted().toList();
        }
    }

    @Test
    void playsTheRequestedGamesAndWritesCompleteChunks() throws Exception {
        SelfPlayRunner.Summary summary = runner.run(options(out, 30, 10, 0, null));

        assertEquals("COMPLETED", summary.stoppedReason());
        assertEquals(30, summary.gamesPlayed());
        assertEquals(0, summary.gamesFailed());
        assertEquals(3, chunks().size(), "10 games per chunk");

        long rows = 0;
        Set<Long> games = new HashSet<>();
        for (Path chunk : chunks()) {
            for (DecisionFile.Row row : DecisionFile.read(chunk).rows()) {
                rows++;
                games.add(row.gameId());
            }
        }
        assertEquals(summary.rows(), rows);
        assertEquals(30, games.size(), "every game has its own id");
        try (var files = Files.list(out)) {
            assertTrue(files.noneMatch(f -> f.getFileName().toString().endsWith(".tmp")), "no temporary files left behind");
        }
    }

    @Test
    void aStopFileEndsTheRunCleanlyBeforeAnyGame() throws Exception {
        Path stop = out.resolve("STOP");
        Files.createFile(stop);
        SelfPlayRunner.Summary summary = runner.run(options(out, 1000, 10, 0, stop));

        assertEquals("STOP_FILE", summary.stoppedReason());
        assertEquals(0, summary.gamesPlayed());
    }

    @Test
    void theTimeLimitEndsTheRunAndKeepsTheGamesPlayedSoFar() throws Exception {
        SelfPlayRunner.Summary summary = runner.run(options(out, 1_000_000, 5, 3, null));

        assertEquals("TIME_LIMIT", summary.stoppedReason());
        assertTrue(summary.gamesPlayed() > 0 && summary.gamesPlayed() < 1_000_000, "games: " + summary.gamesPlayed());
        // the last, incomplete chunk is finished too: nothing that was played is lost
        long rowsInFiles = 0;
        for (Path chunk : chunks()) {
            rowsInFiles += DecisionFile.read(chunk).rows().size();
        }
        assertEquals(summary.rows(), rowsInFiles);
    }

    @Test
    void aRestartContinuesNumberingAndRemovesLeftoversOfTheDeadProcess() throws Exception {
        runner.run(options(out, 10, 5, 0, null));
        assertEquals(2, chunks().size());
        byte[] firstChunk = Files.readAllBytes(chunks().getFirst());
        Files.writeString(out.resolve("PICK_PHASE.000003.ares.tmp"), "half written garbage");

        SelfPlayRunner.Summary second = runner.run(options(out, 10, 5, 0, null));

        assertEquals("COMPLETED", second.stoppedReason());
        assertEquals(4, chunks().size());
        assertTrue(chunks().get(2).getFileName().toString().contains(".000003."), chunks().toString());
        assertEquals(java.util.Arrays.hashCode(firstChunk), java.util.Arrays.hashCode(Files.readAllBytes(chunks().getFirst())), "old chunk untouched");
        try (var files = Files.list(out)) {
            assertTrue(files.noneMatch(f -> f.getFileName().toString().endsWith(".tmp")), "garbage of the dead process removed");
        }
        Set<Long> ids = new HashSet<>();
        for (Path chunk : chunks()) {
            for (DecisionFile.Row row : DecisionFile.read(chunk).rows()) {
                ids.add(row.gameId());
            }
        }
        assertEquals(20, ids.size(), "game ids are unique over restarts");
    }

    @Test
    void matchupsMixDifferentContexts() throws Exception {
        runner.run(new SelfPlayRunner.Options(out, 40, 4, SelfPlayMain.matchups("AGENT:AGENT:1,AGENT_R:AGENT_R:1,AGENT:AGENT_R:1"), 20, 0, null, 0.05, 0));
        Set<Integer> contexts = new HashSet<>();
        for (Path chunk : chunks()) {
            for (DecisionFile.Row row : DecisionFile.read(chunk).rows()) {
                contexts.add(row.context());
            }
        }
        assertEquals(Set.of(0, 1), contexts);
    }

    @Test
    void theProcessStopsByItselfWhenTheDiskIsAlmostFull() throws Exception {
        // a requirement that no disk can satisfy: the run must end cleanly on its own, without playing
        SelfPlayRunner.Summary summary = runner.run(new SelfPlayRunner.Options(out, 1000, 4,
                List.of(new SelfPlayRunner.Matchup(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT, 1)), 10, 0, null, 0.05, 1e9));

        assertEquals("DISK_LOW", summary.stoppedReason());
        assertTrue(summary.gamesPlayed() <= 8, "at most the games that were already started: " + summary.gamesPlayed());
    }

    // -------------------------------------------------------------- command line handling

    @Test
    void matchupSpecificationIsParsedAndValidated() {
        List<SelfPlayRunner.Matchup> parsed = SelfPlayMain.matchups("AGENT:AGENT_R:0.25, SMART:SMART:3");
        assertEquals(2, parsed.size());
        assertEquals(PlayerDifficulty.AGENT_R, parsed.get(0).b());
        assertEquals(3.0, parsed.get(1).weight());
        assertThrows(IllegalArgumentException.class, () -> SelfPlayMain.matchups("AGENT:AGENT"));
        assertThrows(IllegalArgumentException.class, () -> SelfPlayMain.matchups("AGENT:AGENT:0"));
        assertThrows(IllegalArgumentException.class, () -> SelfPlayMain.matchups("AGENT:NOBODY:1"));
    }

    @Test
    void argumentsMustBeNameValuePairs() {
        assertEquals("5", SelfPlayMain.parse(new String[]{"--games", "5"}).get("games"));
        assertThrows(IllegalArgumentException.class, () -> SelfPlayMain.parse(new String[]{"games", "5"}));
        assertThrows(IllegalArgumentException.class, () -> SelfPlayMain.parse(new String[]{"--games"}));
        assertFalse(SelfPlayMain.parse(new String[]{}).containsKey("games"));
    }
}
