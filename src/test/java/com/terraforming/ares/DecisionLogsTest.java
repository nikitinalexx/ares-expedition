package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.agent.DecisionFile;
import com.terraforming.ares.services.agent.DecisionLogs;
import com.terraforming.ares.services.agent.PickPhaseDecisions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The recorded decisions must be attached to the right player and to the right result - a label that belongs to the other
 * player silently ruins every network trained on the data.
 */
class DecisionLogsTest extends EngineTestBase {
    @TempDir
    Path dir;

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    WinPointsService winPointsService;
    @Autowired
    DecisionLogs decisionLogs;

    private static final int GAMES = 12;

    private static List<Path> chunks(Path directory) throws IOException {
        try (var files = java.nio.file.Files.list(directory)) {
            return files.filter(f -> f.getFileName().toString().matches("PICK_PHASE\\.\\d+\\.ares")).sorted().toList();
        }
    }

    private static Path firstChunk(Path directory) throws IOException {
        return chunks(directory).getFirst();
    }

    /** All finished chunks of the directory as one list of rows (unfinished .tmp files are ignored, as the readers do). */
    private static DecisionFile.Content readAll(Path directory) throws IOException {
        DecisionFile.Content all = null;
        List<DecisionFile.Row> rows = new ArrayList<>();
        for (Path chunk : chunks(directory)) {
            DecisionFile.Content content = DecisionFile.read(chunk);
            if (all == null) {
                all = content;
            }
            assertEquals(all.schema(), content.schema(), "all chunks share one schema");
            rows.addAll(content.rows());
        }
        return new DecisionFile.Content(all.schema(), all.schemaJson(), rows);
    }

    /**
     * Plays GAMES games (agent as A, as B and on both sides) and returns the final points per game: [pointsA, pointsB].
     */
    private List<int[]> playAndRecord(Path target) {
        decisionLogs.startRecording(target);
        List<int[]> points = new ArrayList<>();
        try {
            for (int i = 0; i < GAMES; i++) {
                PlayerDifficulty a = i % 4 == 1 ? PlayerDifficulty.SMART : PlayerDifficulty.AGENT;
                PlayerDifficulty b = i % 4 == 0 ? PlayerDifficulty.SMART : PlayerDifficulty.AGENT;
                MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                        .playerNames(new ArrayList<>(List.of("A", "B")))
                        .computers(List.of(a, b))
                        .mulligan(true)
                        .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                        .dummyHand(true)
                        .build());
                simulation.processSimulation(game);

                Map<String, Integer> byName = new HashMap<>();
                for (Player player : game.getPlayerUuidToPlayer().values()) {
                    byName.put(player.getName(), winPointsService.countWinPoints(player, game));
                }
                points.add(new int[]{byName.get("A"), byName.get("B")});
                decisionLogs.finish(game);
            }
        } finally {
            decisionLogs.stopRecording();
        }
        return points;
    }

    @Test
    void everyDecisionCarriesTheResultOfItsOwnPlayer() throws IOException {
        List<int[]> points = playAndRecord(dir);
        DecisionFile.Content content = readAll(dir);

        assertTrue(content.rows().size() > GAMES * 15, "rows: " + content.rows().size());
        int draws = 0;
        for (DecisionFile.Row row : content.rows()) {
            int[] p = points.get((int) row.gameId());
            int mine = p[row.seat()];
            int theirs = p[1 - row.seat()];
            assertEquals(mine - theirs, row.margin(), 0f, "margin of game " + row.gameId() + " seat " + row.seat());
            float expectedOutcome = mine > theirs ? 1f : (mine == theirs ? 0.5f : 0f);
            assertEquals(expectedOutcome, row.outcome(), 0f, "outcome of game " + row.gameId() + " seat " + row.seat());
            if (mine == theirs) {
                draws++;
            }
        }
        assertTrue(draws < content.rows().size() / 2, "almost no draws expected");
    }

    @Test
    void bothSeatsOfOneGameAreEachOthersMirror() throws IOException {
        playAndRecord(dir);
        DecisionFile.Content content = readAll(dir);

        Map<Long, Map<Integer, DecisionFile.Row>> firstRowBySeat = new HashMap<>();
        for (DecisionFile.Row row : content.rows()) {
            firstRowBySeat.computeIfAbsent(row.gameId(), g -> new HashMap<>()).putIfAbsent(row.seat(), row);
        }
        int checked = 0;
        for (Map<Integer, DecisionFile.Row> seats : firstRowBySeat.values()) {
            if (seats.size() == 2) {
                checked++;
                assertEquals(-seats.get(0).margin(), seats.get(1).margin(), 0f);
                assertEquals(1f, seats.get(0).outcome() + seats.get(1).outcome(), 0f);
            }
        }
        assertTrue(checked >= GAMES / 2, "games with both seats recorded: " + checked);
    }

    @Test
    void decisionsAreRecordedInOrderAndMatchTheStateTheNextDecisionSees() throws IOException {
        playAndRecord(dir);
        DecisionFile.Content content = readAll(dir);
        int extras = content.schema().obsSize() - PickPhaseDecisions.EXTRAS_SIZE;

        Map<String, DecisionFile.Row> previous = new HashMap<>();
        for (DecisionFile.Row row : content.rows()) {
            String key = row.gameId() + "/" + row.seat();
            DecisionFile.Row before = previous.get(key);
            if (before == null) {
                for (int phase = 0; phase < 5; phase++) {
                    assertEquals(0f, row.observation()[extras + 1 + phase], "no previous phase before the first phase decision");
                }
            } else {
                assertTrue(row.step() > before.step(), "phase decisions of one player stay in order; sales and drafts may sit between them");
                assertTrue(row.round() >= before.round(), "rounds do not go back");
                int phaseChosenBefore = before.candidateIds()[before.chosen()];
                for (int phase = 1; phase <= 5; phase++) {
                    // what the engine says the player did last round must be what the log says the player chose
                    assertEquals(phase == phaseChosenBefore ? 1f : 0f, row.observation()[extras + phase],
                            "game " + row.gameId() + " seat " + row.seat() + " step " + row.step());
                }
                // and it can not be offered again
                for (int c = 0; c < row.candidateCount(); c++) {
                    assertFalse(row.candidateIds()[c] == phaseChosenBefore, "the previous phase is not a candidate");
                }
            }
            previous.put(key, row);
        }
    }

    @Test
    void noRecordingMeansNoWork() {
        assertFalse(decisionLogs.isRecording());
        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY)
                .dummyHand(true).build());
        simulation.processSimulation(game);
        decisionLogs.finish(game); // must not fail or write anything
        try (var files = java.nio.file.Files.list(dir)) {
            assertEquals(0, files.count(), "nothing is written when recording is off");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void pythonReaderAgreesWithJava() throws Exception {
        playAndRecord(dir);
        Path file = firstChunk(dir);
        DecisionFile.Content content = DecisionFile.read(file);

        Path script = Path.of("ml", "check_decision_file.py").toAbsolutePath();
        Process process;
        try {
            process = new ProcessBuilder("python", script.toString(), file.toString()).redirectErrorStream(true).start();
        } catch (IOException noPython) {
            Assumptions.assumeTrue(false, "python is not available");
            return;
        }
        String output = new String(process.getInputStream().readAllBytes());
        assertTrue(process.waitFor(60, TimeUnit.SECONDS));
        if (output.contains("No module named 'numpy'")) {
            Assumptions.assumeTrue(false, "numpy is not installed");
        }
        assertEquals(0, process.exitValue(), output);

        double outcomeSum = content.rows().stream().mapToDouble(DecisionFile.Row::outcome).sum();
        assertTrue(output.contains("rows=" + content.rows().size() + "\n") || output.contains("rows=" + content.rows().size() + "\r"), output);
        assertTrue(output.contains(String.format(java.util.Locale.ROOT, "sum_outcome=%.1f", outcomeSum)), output);
        assertTrue(output.contains("problems=[]"), output);
    }

    private MarsGame playOneAgentGame() {
        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT_R))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true)
                .build());
        simulation.processSimulation(game);
        return game;
    }

    @Test
    void rotationProducesCompleteChunksAndKeepsEachGameInOneChunk() throws IOException {
        decisionLogs.startRecording(dir);
        for (int i = 0; i < 9; i++) {
            decisionLogs.finish(playOneAgentGame());
            if (i % 3 == 2) {
                decisionLogs.rotate();
            }
        }
        decisionLogs.stopRecording();

        List<Path> chunks = chunks(dir);
        assertEquals(3, chunks.size());
        try (var files = java.nio.file.Files.list(dir)) {
            assertTrue(files.noneMatch(f -> f.getFileName().toString().endsWith(".tmp")), "no temporary files after a clean stop");
        }
        java.util.Set<Long> seenGames = new java.util.HashSet<>();
        for (Path chunk : chunks) {
            java.util.Set<Long> gamesInChunk = new java.util.HashSet<>();
            for (DecisionFile.Row row : DecisionFile.read(chunk).rows()) {
                gamesInChunk.add(row.gameId());
            }
            assertEquals(3, gamesInChunk.size(), "three games per chunk");
            for (Long game : gamesInChunk) {
                assertTrue(seenGames.add(game), "game " + game + " is split over several chunks");
            }
        }
    }

    @Test
    void contextOfTheRowIsTheContextOfTheDecisionMaker() throws IOException {
        decisionLogs.startRecording(dir);
        for (int i = 0; i < 6; i++) {
            decisionLogs.finish(playOneAgentGame()); // A = AGENT (context 0), B = AGENT_R (context 1)
        }
        decisionLogs.stopRecording();

        for (DecisionFile.Row row : readAll(dir).rows()) {
            assertEquals(row.seat(), row.context(), "seat 0 is AGENT (SMART rest), seat 1 is AGENT_R (RANDOM rest)");
        }
    }

    @Test
    void aCrashLeavesOnlyTemporaryFilesAndARestartNeverOverwritesFinishedChunks() throws IOException {
        decisionLogs.startRecording(dir);
        decisionLogs.finish(playOneAgentGame());
        decisionLogs.rotate(); // chunk 1 is complete
        decisionLogs.finish(playOneAgentGame()); // chunk 2 is open ...
        decisionLogs.abort(); // ... and the process dies here

        assertEquals(1, chunks(dir).size(), "only the finished phase chunk is visible to readers");
        try (var files = java.nio.file.Files.list(dir)) {
            List<String> open = files.map(f -> f.getFileName().toString()).filter(n -> n.endsWith(".tmp")).sorted().toList();
            assertTrue(open.containsAll(List.of(
                    "MULLIGAN.000002.ares.tmp", "PICK_CORPORATION.000002.ares.tmp", "PICK_PHASE.000002.ares.tmp")), open.toString());
            assertTrue(open.stream().allMatch(name -> name.matches(
                    "(MULLIGAN|PICK_CORPORATION|PICK_PHASE|DRAFT_CARDS|DISCARD_CARDS|SELL_CARDS|SELL_CARDS_LAST_ROUND|STANDARD_ACTION|BUILD_CARD|BUILD_PAYMENT|BUILD_INPUT|PERFORM_BLUE_ACTION|BLUE_INPUT|UNMI_RT|COLLECT_INCOME)\\.000002\\.ares\\.tmp")),
                    open.toString());
        }
        byte[] chunkOne = java.nio.file.Files.readAllBytes(chunks(dir).getFirst());

        // new process: the stale .tmp is not our business (the pipeline deletes it), numbering must continue after chunk 1
        decisionLogs.startRecordingAfterExisting(dir);
        decisionLogs.finish(playOneAgentGame());
        decisionLogs.stopRecording();

        List<Path> chunks = chunks(dir);
        assertEquals(2, chunks.size());
        assertTrue(chunks.get(1).getFileName().toString().contains(".000002."), chunks.toString());
        assertEquals(java.util.Arrays.hashCode(chunkOne), java.util.Arrays.hashCode(java.nio.file.Files.readAllBytes(chunks.getFirst())), "finished chunk untouched");
    }
}
