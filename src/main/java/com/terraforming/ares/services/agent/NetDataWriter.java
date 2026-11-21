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

    private final Map<MarsGame, List<LinkedHashSet<FloatArrayWrapper>>> collected =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<MarsGame, Integer> lastTurn = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<MarsGame, Long> gameIds = Collections.synchronizedMap(new IdentityHashMap<>());
    private final java.util.concurrent.atomic.AtomicLong nextGameId = new java.util.concurrent.atomic.AtomicLong();

    private final Object fileLock = new Object();
    private BufferedWriter out;
    private long rows;

    public boolean enabled() {
        return System.getProperty("ares.netdata.dir", "").trim().length() > 0;
    }

    /**
     * Called after every processed step (see {@code SimulationProcessorService.processSimulation(game, afterStep)}). A row is taken once per ROUND per
     * player: the value is queried inside the round, but the between-rounds state is the one every round produces and it keeps the corpus small.
     */
    public void afterStep(MarsGame game) {
        Integer last = lastTurn.get(game);
        if (last != null && last == game.getTurns()) {
            return;
        }
        lastTurn.put(game, game.getTurns());
        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        if (players.size() != 2) {
            return;
        }
        List<LinkedHashSet<FloatArrayWrapper>> slots = collected.computeIfAbsent(game, g -> {
            List<LinkedHashSet<FloatArrayWrapper>> list = new ArrayList<>(2);
            list.add(new LinkedHashSet<>());
            list.add(new LinkedHashSet<>());
            return list;
        });
        gameIds.computeIfAbsent(game, g -> nextGameId.incrementAndGet());
        for (int i = 0; i < 2; i++) {
            float[] vector = advancedAiDataCollectionService.collectData(game, players.get(i).getUuid());
            slots.get(i).add(new FloatArrayWrapper(vector));
        }
    }

    /** Labels every collected row of this game with its outcome and writes it out. */
    public void finish(MarsGame game) {
        List<LinkedHashSet<FloatArrayWrapper>> slots = collected.remove(game);
        lastTurn.remove(game);
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
        }
    }
}
