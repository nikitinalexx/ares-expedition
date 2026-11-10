package com.terraforming.ares.selfplay;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.DecisionLogs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Plays games and records the decisions of the agents in finished chunk files. Designed to be killed at any moment:
 * only the chunk that is open at that time is lost (it stays a {@code .tmp} file), everything finished is valid.
 * <p>
 * Every problem is written to {@code <out>/errors.log} (and to stderr) together with the stack trace and the game setup,
 * so that the morning log tells what to fix.
 */
@Component
@RequiredArgsConstructor
public class SelfPlayRunner {
    /**
     * @param a      difficulty of the first seat
     * @param b      difficulty of the second seat (seats are swapped randomly every game)
     * @param weight relative frequency
     */
    public record Matchup(PlayerDifficulty a, PlayerDifficulty b, double weight) {
    }

    public record Options(Path out, int games, int threads, List<Matchup> matchups, int gamesPerChunk,
                          long maxSeconds, Path stopFile, double maxFailureRatio, double minFreeGb) {
    }

    public record Summary(String stoppedReason, int gamesRequested, int gamesPlayed, int gamesFailed, long rows, long chunks,
                          double seconds, Map<String, Integer> matchups) {
    }

    private final GameService gameService;
    private final SimulationProcessorService simulationProcessorService;
    private final DecisionLogs decisionLogs;

    public Summary run(Options options) throws IOException, InterruptedException {
        Files.createDirectories(options.out());
        deleteStaleTemporaryFiles(options.out());
        decisionLogs.startRecordingAfterExisting(options.out());

        long start = System.nanoTime();
        AtomicInteger played = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger attempted = new AtomicInteger();
        Map<String, AtomicInteger> perMatchup = new TreeMap<>();
        for (Matchup matchup : options.matchups()) {
            perMatchup.put(label(matchup), new AtomicInteger());
        }
        AtomicLong lastLog = new AtomicLong(System.currentTimeMillis());
        String stoppedReason = "COMPLETED";

        ExecutorService pool = Executors.newFixedThreadPool(options.threads());
        Semaphore inFlight = new Semaphore(options.threads() * 2);
        try {
            for (int i = 0; i < options.games(); i++) {
                String reason = stopReason(options, start, failed.get(), attempted.get());
                if (reason != null) {
                    stoppedReason = reason;
                    break;
                }
                inFlight.acquire();
                attempted.incrementAndGet();
                pool.submit(() -> {
                    Matchup matchup = pick(options.matchups());
                    boolean swap = ThreadLocalRandom.current().nextBoolean();
                    PlayerDifficulty first = swap ? matchup.b() : matchup.a();
                    PlayerDifficulty second = swap ? matchup.a() : matchup.b();
                    MarsGame game = null;
                    try {
                        game = gameService.createNewSimulation(GameParameters.builder()
                                .playerNames(new ArrayList<>(List.of("Seat1", "Seat2")))
                                .computers(List.of(first, second))
                                .mulligan(true)
                                .expansion(Expansion.BASE).expansion(Expansion.BUFFED_CORPORATION).expansion(Expansion.DISCOVERY)
                                .dummyHand(true)
                                .build());
                        simulationProcessorService.processSimulation(game);
                        decisionLogs.finish(game);
                        perMatchup.get(label(matchup)).incrementAndGet();
                        int done = played.incrementAndGet();
                        if (done % options.gamesPerChunk() == 0) {
                            decisionLogs.rotate();
                        }
                    } catch (Throwable t) {
                        failed.incrementAndGet();
                        if (game != null) {
                            decisionLogs.discard(game);
                        }
                        logError(options.out(), "game " + first + " vs " + second + " failed", t);
                    } finally {
                        inFlight.release();
                    }
                    long now = System.currentTimeMillis();
                    long last = lastLog.get();
                    if (now - last >= 15_000 && lastLog.compareAndSet(last, now)) {
                        double seconds = (System.nanoTime() - start) / 1e9;
                        System.out.printf("[SELFPLAY] %d/%d games, %d failed, %.1f games/s, %d rows, %d chunks%n",
                                played.get(), options.games(), failed.get(), played.get() / seconds, decisionLogs.rowsWritten(), decisionLogs.chunksFinished());
                    }
                });
            }
        } finally {
            pool.shutdown();
            if (!pool.awaitTermination(300, TimeUnit.SECONDS)) {
                logError(options.out(), "games still running 300 s after the stop, they are abandoned", null);
                pool.shutdownNow();
            }
            decisionLogs.stopRecording(); // finishes the last chunk
        }
        if (stoppedReason.equals("COMPLETED") && failed.get() > 0 && failureRatioTooHigh(options, failed.get(), attempted.get())) {
            stoppedReason = "TOO_MANY_FAILURES";
        }

        Map<String, Integer> counts = new TreeMap<>();
        perMatchup.forEach((k, v) -> counts.put(k, v.get()));
        Summary summary = new Summary(stoppedReason, options.games(), played.get(), failed.get(), decisionLogs.rowsWritten(),
                decisionLogs.chunksFinished(), (System.nanoTime() - start) / 1e9, counts);
        writeSummary(options.out(), summary);
        return summary;
    }

    private static String stopReason(Options options, long startNanos, int failed, int attempted) {
        if (options.stopFile() != null && Files.exists(options.stopFile())) {
            return "STOP_FILE";
        }
        if (options.maxSeconds() > 0 && (System.nanoTime() - startNanos) / 1e9 >= options.maxSeconds()) {
            return "TIME_LIMIT";
        }
        if (failureRatioTooHigh(options, failed, attempted)) {
            return "TOO_MANY_FAILURES";
        }
        if (diskIsLow(options)) {
            return "DISK_LOW";
        }
        return null;
    }

    // Defence in depth: this process stops by itself before the disk is full, even if nobody supervises it.
    // Checked at most every few seconds, a chunk is only a few dozen MB.
    private static volatile long lastDiskCheck;
    private static volatile boolean lastDiskLow;

    static boolean diskIsLow(Options options) {
        if (options.minFreeGb() <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastDiskCheck < 3000) {
            return lastDiskLow;
        }
        lastDiskCheck = now;
        try {
            lastDiskLow = Files.getFileStore(options.out()).getUsableSpace() / 1e9 < options.minFreeGb();
        } catch (IOException e) {
            lastDiskLow = true; // can not even ask: do not take the risk
        }
        return lastDiskLow;
    }

    // a systematic problem (not a rare bug of a single game) must stop the process early instead of burning the night
    private static boolean failureRatioTooHigh(Options options, int failed, int attempted) {
        return failed >= 10 && attempted > 0 && failed / (double) attempted > options.maxFailureRatio();
    }

    private static Matchup pick(List<Matchup> matchups) {
        double total = matchups.stream().mapToDouble(Matchup::weight).sum();
        double x = ThreadLocalRandom.current().nextDouble() * total;
        for (Matchup matchup : matchups) {
            x -= matchup.weight();
            if (x <= 0) {
                return matchup;
            }
        }
        return matchups.getLast();
    }

    private static String label(Matchup matchup) {
        return matchup.a() + "-" + matchup.b();
    }

    private static void deleteStaleTemporaryFiles(Path out) throws IOException {
        try (var files = Files.list(out)) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".tmp")).toList()) {
                Files.delete(file);
            }
        }
    }

    private static void writeSummary(Path out, Summary summary) throws IOException {
        StringBuilder json = new StringBuilder("{");
        json.append("\"stoppedReason\":\"").append(summary.stoppedReason()).append("\",");
        json.append("\"gamesRequested\":").append(summary.gamesRequested()).append(',');
        json.append("\"gamesPlayed\":").append(summary.gamesPlayed()).append(',');
        json.append("\"gamesFailed\":").append(summary.gamesFailed()).append(',');
        json.append("\"rows\":").append(summary.rows()).append(',');
        json.append("\"chunks\":").append(summary.chunks()).append(',');
        json.append("\"seconds\":").append(String.format(java.util.Locale.ROOT, "%.1f", summary.seconds())).append(',');
        json.append("\"matchups\":{");
        boolean first = true;
        for (var e : summary.matchups().entrySet()) {
            json.append(first ? "" : ",").append('"').append(e.getKey()).append("\":").append(e.getValue());
            first = false;
        }
        json.append("}}");
        Path tmp = out.resolve("summary.json.tmp");
        Files.writeString(tmp, json.toString());
        Files.move(tmp, out.resolve("summary-" + Instant.now().toEpochMilli() + ".json"), StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Appends the problem to {@code errors.log} and writes it to stderr: what happened, when and the whole stack trace.
     */
    static void logError(Path out, String message, Throwable t) {
        StringWriter text = new StringWriter();
        PrintWriter writer = new PrintWriter(text);
        writer.println("[" + Instant.now() + "] " + message);
        if (t != null) {
            t.printStackTrace(writer);
        }
        writer.println();
        writer.flush();
        System.err.print(text);
        try {
            Files.writeString(out.resolve("errors.log"), text.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("could not write errors.log: " + e);
        }
    }
}
