package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.simulations.FloatArrayWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Collects the NETWORK_V2 dataset while games are played by the owner's processors.
 * <p>
 * Why (owner, 21/11): the value that makes NETWORK_V2 play must be trained on the vector its processors actually see - the 898-value V2 encoder
 * ({@link AdvancedAiDataCollectionService}: table 417 + hand 481, and the hand is the part that makes it strong). This is the app-side half of
 * `advanced_ai`'s `runSimulationWithDataset`: after every step the 898-vector of both players is collected (deduplicated per game, like
 * `GameResult`'s LinkedHashSet), and at GAME_END each row is labelled with the winner. `ml/train_network_v2.py` trains on the CSV and exports a
 * `coefficients.bin` zip that {@code NNService} loads through {@code PureJavaValueNetwork}.
 * <p>
 * Enabled with {@code -Dares.netdata.dir=DIR}; off by default and free when off.
 */
@Service
@RequiredArgsConstructor
public class NetDataWriter {

    private final AdvancedAiDataCollectionService advancedAiDataCollectionService;
    private final WinPointsService winPointsService;

    private final Map<MarsGame, List<List<FloatArrayWrapper>>> collected =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<MarsGame, List<LinkedHashSet<FloatArrayWrapper>>> unique =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<MarsGame, Long> gameIds = Collections.synchronizedMap(new IdentityHashMap<>());
    private final java.util.concurrent.atomic.AtomicLong nextGameId = new java.util.concurrent.atomic.AtomicLong();

    private final Object fileLock = new Object();
    private BufferedWriter out;
    private java.io.OutputStream bin;
    private java.nio.ByteBuffer binaryRow;
    private long rows;
    private long steps;

    /**
     * Binary by default: the owner trained on tens of millions of states, and the text CSV is ~3.6 KB per row (100+ GB and
     * tens of minutes of parsing per pass). The record is little-endian `int32 gameId, float16 label, float16 x 898`
     * behind an 8-byte magic and a uint16 dimension (`ml/train_network_v2.py` reads it, `NetDataWriterTest` pins the bytes).
     * `-Dares.netdata.format=csv` keeps the old text file for debugging.
     */
    private boolean binary() {
        return !"csv".equalsIgnoreCase(System.getProperty("ares.netdata.format", "bin"));
    }

    public static final byte[] MAGIC = {'N', 'D', '2', 'B', 'I', 'N', '0', '1'};

    /** One record of `netdata.bin`, the only place the layout is written: the test parses what this produces. */
    static java.nio.ByteBuffer encodeRow(java.nio.ByteBuffer buffer, int gameId, float label, float[] data) {
        buffer.clear();
        buffer.putInt(gameId);
        buffer.putShort(Float.floatToFloat16(label));
        for (float value : data) {
            buffer.putShort(Float.floatToFloat16(value));
        }
        return buffer;
    }

    public boolean enabled() {
        return System.getProperty("ares.netdata.dir", "").trim().length() > 0;
    }

    private boolean dedup() {
        return Boolean.parseBoolean(System.getProperty("ares.netdata.dedup", "true"));
    }

    /**
     * Called after every processed step (see {@code SimulationProcessorService.processSimulation(game, afterStep)}). Records the state of both
     * players after EVERY step, like the owner logged it (blue actions, phase-5 discards, each card played): measured 1123 post-states per game over
     * 100 games, 427 distinct after the per-game dedup he also used ("if a state repeated, I did not take it"). One state per round was 66 per game -
     * the corpus was 6x thinner than his at the same game count.
     */
    public void afterStep(MarsGame game) {
        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        if (players.size() != 2) {
            return;
        }
        List<List<FloatArrayWrapper>> slots = collected.computeIfAbsent(game, g -> {
            List<List<FloatArrayWrapper>> list = new ArrayList<>(2);
            list.add(new ArrayList<>());
            list.add(new ArrayList<>());
            return list;
        });
        boolean dedup = dedup();
        List<LinkedHashSet<FloatArrayWrapper>> uniqueSlots = dedup
                ? unique.computeIfAbsent(game, g -> {
                    List<LinkedHashSet<FloatArrayWrapper>> list = new ArrayList<>(2);
                    list.add(new LinkedHashSet<>());
                    list.add(new LinkedHashSet<>());
                    return list;
                })
                : null;
        gameIds.computeIfAbsent(game, g -> nextGameId.incrementAndGet());
        for (int i = 0; i < 2; i++) {
            float[] vector = advancedAiDataCollectionService.collectData(game, players.get(i).getUuid());
            FloatArrayWrapper state = new FloatArrayWrapper(vector);
            if (uniqueSlots == null || uniqueSlots.get(i).add(state)) {
                slots.get(i).add(state);
            }
            steps++;
        }
    }

    /** Labels every collected row of this game with its outcome and writes it out. */
    public void finish(MarsGame game) {
        List<List<FloatArrayWrapper>> slots = collected.remove(game);
        unique.remove(game);
        Long gameId = gameIds.remove(game);
        if (slots == null || !enabled()) {
            return;
        }
        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        int first = winPointsService.countWinPoints(players.get(0), game);
        int second = winPointsService.countWinPoints(players.get(1), game);
        float outcome = first == second ? 0.5f : (first > second ? 1f : 0f);
        try {
            synchronized (fileLock) {
                if (binary()) {
                    if (bin == null) {
                        Path dir = Path.of(System.getProperty("ares.netdata.dir"));
                        Files.createDirectories(dir);
                        java.io.OutputStream file = Files.newOutputStream(dir.resolve("netdata.bin"),
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        bin = new java.io.BufferedOutputStream(file, 1 << 20);
                        if (Files.size(dir.resolve("netdata.bin")) == 0) {
                            int dimension = slots.get(0).isEmpty() ? (slots.get(1).isEmpty() ? 0 : slots.get(1).get(0).data().length)
                                    : slots.get(0).get(0).data().length;
                            if (dimension == 0) {
                                return; // no rows in this game: emitting a zero-dimension header would poison the file
                            }
                            bin.write(MAGIC);
                            bin.write(new byte[]{(byte) (dimension & 0xff), (byte) ((dimension >> 8) & 0xff)});
                        }
                    }
                    for (int i = 0; i < 2; i++) {
                        float label = outcome == 0.5f ? 0.5f : (i == 0 ? outcome : 1f - outcome);
                        for (FloatArrayWrapper state : slots.get(i)) {
                            float[] data = state.data();
                            if (binaryRow == null) {
                                binaryRow = java.nio.ByteBuffer.allocate(4 + 2 + 2 * data.length)
                                        .order(java.nio.ByteOrder.LITTLE_ENDIAN);
                            }
                            encodeRow(binaryRow, gameId == null ? 0 : gameId.intValue(), label, data);
                            bin.write(binaryRow.array(), 0, binaryRow.position());
                            rows++;
                        }
                    }
                    bin.flush();
                    return;
                }
                if (out == null) {
                    Path dir = Path.of(System.getProperty("ares.netdata.dir"));
                    Files.createDirectories(dir);
                    out = Files.newBufferedWriter(dir.resolve("netdata.csv"), StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
                for (int i = 0; i < 2; i++) {
                    float label = outcome == 0.5f ? 0.5f : (i == 0 ? outcome : 1f - outcome);
                    for (FloatArrayWrapper state : slots.get(i)) {
                        StringBuilder line = new StringBuilder();
                        line.append(gameId == null ? 0L : gameId).append(',');
                        line.append(label);
                        line.append(',');
                        for (float value : state.data()) {
                            line.append(value).append(',');
                        }
                        line.setLength(line.length() - 1);
                        out.write(line.toString());
                        out.newLine();
                        rows++;
                    }
                }
            }
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    public long rows() {
        return rows;
    }

    /** Post-states seen by the hook, before the per-game deduplication: rows/steps is what the dedup throws away. */
    public long steps() {
        return steps;
    }

    /** Closes the file (called by the runner when the batch is done). */
    public void close() {
        synchronized (fileLock) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                    // a writer that cannot close has nothing left to say
                }
                out = null;
            }
            if (bin != null) {
                try {
                    bin.close();
                } catch (IOException ignored) {
                    // same
                }
                bin = null;
            }
        }
    }
}
