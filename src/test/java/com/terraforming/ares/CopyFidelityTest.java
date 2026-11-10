package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The AI evaluates every candidate move on {@code new MarsGame(game)}. If the copy constructor drops a field or shares
 * a mutable object with the original, projections are computed on a wrong state (or, worse, corrupt the real game).
 * <p>
 * Plays full games and, after every step, compares a reflective dump of the game and its copy and looks for shared
 * mutable objects. Known/intentional differences are listed in {@link #KNOWN_DIFFERENCES} / {@link #KNOWN_SHARED}.
 */
class CopyFidelityTest extends EngineTestBase {
    // normalized path prefixes (indices replaced by [*]) that the copy intentionally does not preserve
    static final Set<String> KNOWN_DIFFERENCES = Set.of(
            "$.corporationsDeck",                       // not used outside MarsGame itself
            "$.playerUuidToPlayer[*].played.cardToTurn" // Deck copy skips it; only read by statistics endpoints, never by rules/AI
    );
    // shared but never mutated anywhere in the code base (verified by grep); if one starts being mutated, copy it deeply
    static final Set<String> KNOWN_SHARED = Set.of(
            "$.planet.measurableGlobalParameters[*].levels",
            "$.playerUuidToPlayer[*].builds",
            "$.playerUuidToPlayer[*].corporations",
            "$.playerUuidToPlayer[*].nextTurns"
    );

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;

    @Test
    void copyIsEqualAndIndependent() {
        Map<String, String> diffExamples = new TreeMap<>();
        Map<String, Integer> diffCounts = new TreeMap<>();
        Map<String, String> sharedExamples = new TreeMap<>();

        for (PlayerDifficulty difficulty : List.of(PlayerDifficulty.RANDOM, PlayerDifficulty.SMART)) {
            for (int g = 0; g < 3; g++) {
                MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                        .playerNames(List.of("A", "B"))
                        .computers(List.of(difficulty, difficulty))
                        .mulligan(true)
                        .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                        .dummyHand(true)
                        .build());
                simulation.processSimulation(game, current -> {
                    MarsGame copy = new MarsGame(current);
                    for (String d : ObjectGraphs.diff(ObjectGraphs.dump(current), ObjectGraphs.dump(copy))) {
                        String key = normalize(d.substring(0, d.indexOf(':')));
                        diffCounts.merge(key, 1, Integer::sum);
                        diffExamples.putIfAbsent(key, d);
                    }
                    for (String s : ObjectGraphs.sharedMutableObjects(current, copy)) {
                        sharedExamples.putIfAbsent(normalize(s.substring(0, s.indexOf(' '))), s);
                    }
                });
            }
        }

        List<String> unexpectedDiffs = new ArrayList<>();
        diffExamples.forEach((k, v) -> {
            if (KNOWN_DIFFERENCES.stream().noneMatch(k::startsWith)) {
                unexpectedDiffs.add(k + "  x" + diffCounts.get(k) + "   e.g. " + v);
            }
        });
        List<String> unexpectedShared = new ArrayList<>();
        sharedExamples.forEach((k, v) -> {
            if (KNOWN_SHARED.stream().noneMatch(k::startsWith)) {
                unexpectedShared.add(v);
            }
        });

        System.out.println("=== COPY DIFFERENCES (" + unexpectedDiffs.size() + ") ===");
        unexpectedDiffs.forEach(System.out::println);
        System.out.println("=== SHARED MUTABLE OBJECTS (" + unexpectedShared.size() + ") ===");
        unexpectedShared.forEach(System.out::println);

        assertTrue(unexpectedDiffs.isEmpty(), "Copy differs from original: " + unexpectedDiffs);
        assertTrue(unexpectedShared.isEmpty(), "Copy shares mutable objects with original: " + unexpectedShared);
    }

    private static String normalize(String path) {
        return path.replaceAll("\\[[^\\]]*\\]", "[*]");
    }
}
