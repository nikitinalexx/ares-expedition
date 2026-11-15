package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.agent.DecisionFile;
import com.terraforming.ares.services.agent.DecisionLogs;
import com.terraforming.ares.services.agent.DecisionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The afterstate stream the value net is trained on: every agent action also writes the state one action later, with the
 * same game outcome. Without it the value net only knows decision states, while the one-ply search reads the state right
 * after a candidate was applied - a different distribution, which is why the search could not price a card.
 * <p>
 * Invariants: afterstate rows exist; their observations are (almost always) also decision observations of the same player
 * later in the same game; their outcome belongs to their own seat; and they are not capped like decisions.
 */
class AfterstateRecordingTest extends EngineTestBase {
    private static final int GAMES = 4;
    private static final int ROW_CAP = 8;

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    DecisionLogs decisionLogs;
    @Autowired
    WinPointsService winPointsService;

    @TempDir
    Path dir;

    private List<int[]> playAndRecord() {
        return playAndRecord(0);
    }

    /** @param rowCap 0 = record every decision (needed to recognise the afterstates among them) */
    private List<int[]> playAndRecord(int rowCap) {
        decisionLogs.startRecording(dir);
        decisionLogs.setMaxRowsPerGameType(rowCap);
        List<int[]> points = new ArrayList<>();
        try {
            for (int i = 0; i < GAMES; i++) {
                MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                        .playerNames(new ArrayList<>(List.of("A", "B")))
                        .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                        .mulligan(true)
                        .expansion(com.terraforming.ares.model.Expansion.BASE)
                        .expansion(com.terraforming.ares.model.Expansion.DISCOVERY)
                        .expansion(com.terraforming.ares.model.Expansion.BUFFED_CORPORATION)
                        .dummyHand(true).build());
                simulation.processSimulation(game);
                decisionLogs.finish(game);
                // the writer orders the seats by player NAME (DecisionLogs.finish), so the test must do the same
                java.util.List<com.terraforming.ares.model.Player> players =
                        new java.util.ArrayList<>(game.getPlayerUuidToPlayer().values());
                players.sort(java.util.Comparator.comparing(com.terraforming.ares.model.Player::getName));
                int[] score = new int[2];
                for (int seat = 0; seat < players.size(); seat++) {
                    score[seat] = winPointsService.countWinPoints(players.get(seat), game);
                }
                points.add(score);
            }
        } finally {
            decisionLogs.stopRecording();
        }
        return points;
    }

    private record Row(DecisionType type, int gameId, int seat, int step, float outcome, float[] observation) {
    }

    private List<Row> readAll() throws IOException {
        List<Row> rows = new ArrayList<>();
        try (var files = Files.list(dir)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".ares")).toList()) {
                DecisionFile.Content content = DecisionFile.read(file);
                for (DecisionFile.Row row : content.rows()) {
                    rows.add(new Row(content.schema().type(), (int) row.gameId(), row.seat(), row.step(),
                            row.outcome(), row.observation()));
                }
            }
        }
        return rows;
    }

    @Test
    void everyActionAlsoWritesTheStateOneActionLater() throws IOException {
        List<int[]> points = playAndRecord(0);
        List<Row> rows = readAll();
        List<Row> afterstates = rows.stream().filter(row -> row.type() == DecisionType.AFTERSTATE).toList();
        assertFalse(afterstates.isEmpty(), "no afterstate rows were written at all");

        Map<String, Set<String>> decisions = new HashMap<>();
        for (Row row : rows) {
            if (row.type() != DecisionType.AFTERSTATE) {
                decisions.computeIfAbsent(row.gameId() + "/" + row.seat(), key -> new HashSet<>())
                        .add(Arrays.toString(row.observation()));
            }
        }
        int matched = 0;
        for (Row row : afterstates) {
            if (decisions.getOrDefault(row.gameId() + "/" + row.seat(), Set.of())
                    .contains(Arrays.toString(row.observation()))) {
                matched++;
            }
        }
        assertTrue(matched >= afterstates.size() * 8 / 10,
                "only " + matched + " of " + afterstates.size()
                        + " afterstates are also decision states of the same player: the two distributions differ");

        for (Row row : afterstates) {
            int[] score = points.get(row.gameId());
            int mine = score[row.seat()];
            int theirs = score[1 - row.seat()];
            float expected = mine > theirs ? 1f : (mine == theirs ? 0.5f : 0f);
            assertEquals(expected, row.outcome(), 0f,
                    "an afterstate carries another game's result: game " + row.gameId() + " seat " + row.seat());
        }

    }

    @Test
    void afterstatesAreNotCappedLikeDecisions() throws IOException {
        playAndRecord(ROW_CAP);
        List<Row> afterstates = readAll().stream()
                .filter(row -> row.type() == DecisionType.AFTERSTATE).toList();
        assertFalse(afterstates.isEmpty(), "no afterstate rows with the per-type cap on");
        Map<Integer, Integer> perGame = new HashMap<>();
        for (Row row : afterstates) {
            perGame.merge(row.gameId(), 1, Integer::sum);
        }
        assertTrue(perGame.values().stream().anyMatch(count -> count > ROW_CAP),
                "afterstates look capped like decisions: " + perGame);
    }
}
