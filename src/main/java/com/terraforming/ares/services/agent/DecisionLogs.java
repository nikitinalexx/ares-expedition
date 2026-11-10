package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.WinPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects the decisions of running games and writes them (with the result of the game) when the game is finished.
 * Off by default; {@link #startRecording(Path)} turns it on.
 * <p>
 * Files are written in <b>chunks</b>: {@code <dir>/<TYPE>.<chunk>.ares.tmp} while a chunk is open and renamed to
 * {@code <TYPE>.<chunk>.ares} (atomically) by {@link #rotate()} / {@link #stopRecording()}. A process that dies in the middle
 * leaves only {@code .tmp} files, which readers ignore - every finished chunk is complete and valid. All rows of one game
 * always land in the same chunk.
 * <p>
 * A decision can only be labelled after the game ends, so decisions wait in memory per game until {@link #finish(MarsGame)}.
 * Games that are never finished leak their buffers: always call finish (or {@link #discard(MarsGame)}).
 */
@Service
@RequiredArgsConstructor
public class DecisionLogs {
    private record Pending(String playerUuid, Decision decision, int chosen, int round, int step) {
    }

    private final WinPointsService winPointsService;

    private final Map<MarsGame, List<Pending>> pending = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Object lock = new Object();
    private final Map<DecisionType, DecisionFile.Writer> writers = new EnumMap<>(DecisionType.class);
    private final Map<DecisionType, Path> openFiles = new EnumMap<>(DecisionType.class);
    private final AtomicLong gameCounter = new AtomicLong();
    private final AtomicLong rowsWritten = new AtomicLong();
    private final AtomicLong chunksFinished = new AtomicLong();
    private volatile Path directory;
    private int chunk = 1;
    private long gameIdBase;

    public boolean isRecording() {
        return directory != null;
    }

    public long rowsWritten() {
        return rowsWritten.get();
    }

    public long chunksFinished() {
        return chunksFinished.get();
    }

    public void startRecording(Path dir) {
        synchronized (lock) {
            stopRecording();
            this.directory = dir;
            this.gameCounter.set(0);
            this.gameIdBase = 0;
            this.rowsWritten.set(0);
            this.chunksFinished.set(0);
            this.chunk = 1;
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Continue numbering chunks after the ones already in the directory (a restarted run must never overwrite finished chunks).
     */
    public void startRecordingAfterExisting(Path dir) {
        startRecording(dir);
        synchronized (lock) {
            // ids must stay unique over the whole run although every restart begins counting from zero
            this.gameIdBase = java.util.concurrent.ThreadLocalRandom.current().nextLong() & 0xFFFFFFFF00000000L;
            try (var files = Files.list(dir)) {
                this.chunk = files.map(f -> f.getFileName().toString())
                        .filter(n -> n.endsWith(".ares"))
                        .mapToInt(n -> Integer.parseInt(n.split("\\.")[1]))
                        .max().orElse(0) + 1;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public void stopRecording() {
        synchronized (lock) {
            if (directory != null) {
                closeCurrentChunk();
            }
            directory = null;
        }
        pending.clear();
    }

    /**
     * Simulates the death of the process (tests): streams are closed but nothing is renamed, so the open chunk stays a
     * {@code .tmp} file that readers ignore.
     */
    public void abort() {
        synchronized (lock) {
            for (DecisionFile.Writer writer : writers.values()) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
            writers.clear();
            openFiles.clear();
            directory = null;
        }
        pending.clear();
    }

    /**
     * Finishes the current chunk (all its files become visible to readers) and starts the next one.
     */
    public void rotate() {
        synchronized (lock) {
            if (directory != null) {
                closeCurrentChunk();
            }
        }
    }

    private void closeCurrentChunk() {
        try {
            boolean any = !writers.isEmpty();
            for (var entry : writers.entrySet()) {
                entry.getValue().close();
                Path tmp = openFiles.get(entry.getKey());
                Files.move(tmp, Path.of(tmp.toString().replaceAll("\\.tmp$", "")), StandardCopyOption.ATOMIC_MOVE);
            }
            writers.clear();
            openFiles.clear();
            if (any) {
                chunk++;
                chunksFinished.incrementAndGet();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void record(MarsGame game, Player player, Decision decision, int chosen) {
        if (directory == null) {
            return;
        }
        List<Pending> list = pending.computeIfAbsent(game, g -> new ArrayList<>());
        synchronized (list) {
            int step = (int) list.stream().filter(p -> p.playerUuid().equals(player.getUuid())).count();
            list.add(new Pending(player.getUuid(), decision, chosen, game.getTurns(), step));
        }
    }

    public void discard(MarsGame game) {
        pending.remove(game);
    }

    /**
     * Labels the decisions of a finished game with its result and writes them.
     */
    public void finish(MarsGame game) {
        List<Pending> list = pending.remove(game);
        if (list == null || list.isEmpty() || directory == null) {
            return;
        }
        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        players.sort(Comparator.comparing(Player::getName));
        if (players.size() != 2) {
            throw new IllegalStateException("Recording supports 2 player games only");
        }
        Map<String, Integer> points = new HashMap<>();
        for (Player player : players) {
            points.put(player.getUuid(), winPointsService.countWinPoints(player, game));
        }

        synchronized (lock) {
            if (directory == null) {
                return;
            }
            long gameId = gameIdBase + gameCounter.getAndIncrement();
            try {
                for (Pending p : list) {
                    Player me = game.getPlayerByUuid(p.playerUuid());
                    int myPoints = points.get(me.getUuid());
                    int otherPoints = points.entrySet().stream().filter(e -> !e.getKey().equals(me.getUuid())).findFirst().orElseThrow().getValue();
                    float outcome = myPoints > otherPoints ? 1f : (myPoints == otherPoints ? 0.5f : 0f);
                    Decision d = p.decision();

                    float[][] features = new float[d.candidates().size()][];
                    int[] ids = new int[d.candidates().size()];
                    for (int i = 0; i < ids.length; i++) {
                        ids[i] = d.candidates().get(i).id();
                        features[i] = d.candidates().get(i).features();
                    }
                    writerFor(d).write(new DecisionFile.Row(d.observation(), ids.length, ids, features, p.chosen(),
                            players.indexOf(me), me.getDifficulty().agentContext(), p.round(), outcome, myPoints - otherPoints, gameId, p.step()));
                    rowsWritten.incrementAndGet();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private DecisionFile.Writer writerFor(Decision decision) throws IOException {
        DecisionFile.Writer writer = writers.get(decision.type());
        if (writer == null) {
            DecisionFile.Schema schema = new DecisionFile.Schema(decision.type(), decision.observation().length,
                    decision.type().maxCandidates(), decision.candidates().getFirst().features().length,
                    decision.type().observationLayout());
            Path file = directory.resolve(String.format("%s.%06d.ares.tmp", decision.type(), chunk));
            writer = new DecisionFile.Writer(file, schema);
            writers.put(decision.type(), writer);
            openFiles.put(decision.type(), file);
        }
        return writer;
    }
}
