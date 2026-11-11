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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects the decisions of running games and writes them (with the result of the game) when the game is finished.
 * Off by default; {@link #startRecording(Path)} turns it on.
 * <p>
 * Files are written in <b>chunks</b>: {@code <dir>/<TYPE>.<chunk>.ares.tmp} while a chunk is open and renamed to
 * {@code <TYPE>.<chunk>.ares} (atomically) by auto-rotate / {@link #rotate()} / {@link #stopRecording()}. A process that
 * dies in the middle leaves only {@code .tmp} files, which readers ignore - every finished chunk is complete and valid.
 * All rows of one game always land in the same chunk.
 * <p>
 * Encoding (float16 pack) runs on the caller thread; a dedicated writer only appends bytes and rotates. The write queue
 * is bounded (backpressure), matching the policy_ai DatasetWriter pattern so RAM does not explode when the disk lags.
 */
@Service
@RequiredArgsConstructor
public class DecisionLogs {
    private static final int WRITE_QUEUE_CAPACITY = 256;
    private static final Object STOP = new Object();

    private record Pending(String playerUuid, Decision decision, int chosen, int round, int step) {
    }

    private static final class Buffer {
        final List<Pending> list = new ArrayList<>();
        final Map<String, Integer> steps = new HashMap<>();
    }

    private record EncodedGame(List<EncodedRow> rows) {
    }

    private record EncodedRow(DecisionType type, int obsSize, int maxCandidates, int candidateFeatures,
                              String observationLayout, byte[] bytes) {
    }

    private record RotateSignal(java.util.concurrent.CountDownLatch done) {
    }

    private final WinPointsService winPointsService;

    private final Map<MarsGame, Buffer> pending = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    private final Map<DecisionType, DecisionFile.Writer> writers = new EnumMap<>(DecisionType.class);
    private final Map<DecisionType, Path> openFiles = new EnumMap<>(DecisionType.class);
    private final AtomicLong gameCounter = new AtomicLong();
    private final AtomicLong rowsWritten = new AtomicLong();
    private final AtomicLong chunksFinished = new AtomicLong();
    private final BlockingQueue<Object> writeQueue = new ArrayBlockingQueue<>(WRITE_QUEUE_CAPACITY);
    private volatile Path directory;
    private volatile Thread writerThread;
    private volatile UncheckedIOException writeFailure;
    private int chunk = 1;
    private long gameIdBase;
    /** 0 = only manual {@link #rotate()}; self-play sets this so workers never wait on disk. */
    private int gamesPerChunk;
    private int gamesInChunk;

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
        startRecording(dir, 0);
    }

    public void startRecording(Path dir, int gamesPerChunk) {
        stopRecording();
        synchronized (lock) {
            this.directory = dir;
            this.gameCounter.set(0);
            this.gameIdBase = 0;
            this.rowsWritten.set(0);
            this.chunksFinished.set(0);
            this.chunk = 1;
            this.gamesPerChunk = Math.max(0, gamesPerChunk);
            this.gamesInChunk = 0;
            this.writeFailure = null;
            writeQueue.clear();
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            startWriterThread();
        }
    }

    /**
     * Continue numbering chunks after the ones already in the directory (a restarted run must never overwrite finished chunks).
     */
    public void startRecordingAfterExisting(Path dir) {
        startRecordingAfterExisting(dir, 0);
    }

    public void startRecordingAfterExisting(Path dir, int gamesPerChunk) {
        startRecording(dir, gamesPerChunk);
        synchronized (lock) {
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
        Thread thread = writerThread;
        if (thread != null && thread.isAlive()) {
            try {
                writeQueue.put(STOP);
                thread.join(TimeUnit.MINUTES.toMillis(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                thread.interrupt();
            }
        }
        synchronized (lock) {
            if (directory != null) {
                closeCurrentChunk();
            }
            directory = null;
            writerThread = null;
        }
        pending.clear();
        rethrowWriteFailure();
    }

    /**
     * Simulates the death of the process (tests): streams are closed but nothing is renamed, so the open chunk stays a
     * {@code .tmp} file that readers ignore.
     */
    public void abort() {
        Thread thread = writerThread;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        writeQueue.clear();
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
            writerThread = null;
        }
        pending.clear();
    }

    /**
     * Finishes the current chunk (all its files become visible to readers) and starts the next one.
     */
    public void rotate() {
        rethrowWriteFailure();
        if (directory == null) {
            return;
        }
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        try {
            writeQueue.put(new RotateSignal(done));
            if (!done.await(5, TimeUnit.MINUTES)) {
                throw new IllegalStateException("decision-logs writer did not rotate within 5 minutes");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while rotating decision logs", e);
        }
        rethrowWriteFailure();
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
            gamesInChunk = 0;
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
        Buffer buffer = pending.computeIfAbsent(game, g -> new Buffer());
        synchronized (buffer) {
            int step = buffer.steps.merge(player.getUuid(), 1, Integer::sum) - 1;
            buffer.list.add(new Pending(player.getUuid(), decision, chosen, game.getTurns(), step));
        }
    }

    public void discard(MarsGame game) {
        pending.remove(game);
    }

    /**
     * Labels the decisions of a finished game, encodes them on this thread, and enqueues bytes for the writer.
     */
    public void finish(MarsGame game) {
        rethrowWriteFailure();
        Buffer buffer = pending.remove(game);
        if (buffer == null || directory == null) {
            return;
        }
        List<Pending> list;
        synchronized (buffer) {
            list = buffer.list;
        }
        if (list.isEmpty()) {
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

        long gameId = gameIdBase + gameCounter.getAndIncrement();
        List<EncodedRow> encoded = new ArrayList<>(list.size());
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
            DecisionFile.Schema schema = new DecisionFile.Schema(d.type(), d.observation().length,
                    d.type().maxCandidates(), d.candidates().getFirst().features().length, d.type().observationLayout());
            DecisionFile.Row row = new DecisionFile.Row(d.observation(), ids.length, ids, features, p.chosen(),
                    players.indexOf(me), me.getDifficulty().agentContext(), p.round(), outcome, myPoints - otherPoints, gameId, p.step());
            encoded.add(new EncodedRow(d.type(), schema.obsSize(), schema.maxCandidates(), schema.candidateFeatures(),
                    schema.observationLayout(), DecisionFile.encodeRow(schema, row)));
        }
        EncodedGame item = new EncodedGame(encoded);
        try {
            while (!writeQueue.offer(item, 1, TimeUnit.SECONDS)) {
                rethrowWriteFailure();
                Thread writer = writerThread;
                if (writer == null || !writer.isAlive()) {
                    throw new IllegalStateException("the decision-logs writer is not running");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while enqueueing decision rows", e);
        }
    }

    private void startWriterThread() {
        Thread thread = new Thread(this::writeLoop, "decision-logs-writer");
        thread.setDaemon(true);
        writerThread = thread;
        thread.start();
    }

    private void writeLoop() {
        try {
            while (true) {
                Object item = writeQueue.take();
                if (item == STOP) {
                    synchronized (lock) {
                        if (directory != null) {
                            closeCurrentChunk();
                        }
                        directory = null;
                    }
                    return;
                }
                if (item instanceof RotateSignal signal) {
                    try {
                        synchronized (lock) {
                            if (directory != null) {
                                closeCurrentChunk();
                            }
                        }
                    } finally {
                        signal.done().countDown();
                    }
                    continue;
                }
                EncodedGame game = (EncodedGame) item;
                synchronized (lock) {
                    if (directory == null) {
                        continue;
                    }
                    for (EncodedRow row : game.rows()) {
                        writerFor(row).writeEncoded(row.bytes());
                        rowsWritten.incrementAndGet();
                    }
                    gamesInChunk++;
                    if (gamesPerChunk > 0 && gamesInChunk >= gamesPerChunk) {
                        closeCurrentChunk();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            writeFailure = new UncheckedIOException(e);
        } catch (UncheckedIOException e) {
            writeFailure = e;
        } catch (Throwable e) {
            writeFailure = new UncheckedIOException(new IOException(e));
            com.terraforming.ares.selfplay.FatalErrors.haltIfOutOfMemory(e);
        }
    }

    private DecisionFile.Writer writerFor(EncodedRow item) throws IOException {
        DecisionFile.Writer writer = writers.get(item.type());
        if (writer == null) {
            DecisionFile.Schema schema = new DecisionFile.Schema(item.type(), item.obsSize(), item.maxCandidates(),
                    item.candidateFeatures(), item.observationLayout());
            Path file = directory.resolve(String.format("%s.%06d.ares.tmp", item.type(), chunk));
            writer = new DecisionFile.Writer(file, schema);
            writers.put(item.type(), writer);
            openFiles.put(item.type(), file);
        }
        return writer;
    }

    private void rethrowWriteFailure() {
        UncheckedIOException failure = writeFailure;
        if (failure != null) {
            throw failure;
        }
    }
}
