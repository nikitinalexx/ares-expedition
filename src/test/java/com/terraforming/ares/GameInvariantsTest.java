package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plays many full games (random and smart computers, with and without optional expansions) and checks
 * {@link GameInvariants} after every step. Prints a report of distinct violations with a first example.
 * More games: {@code mvn test -Dtest=GameInvariantsTest -Dinvariants.games=200}.
 */
class GameInvariantsTest extends EngineTestBase {
    /**
     * Open findings (see docs/BUGS.md). They are still printed in the report, but do not fail the build; any NEW kind does.
     */
    static final List<String> KNOWN_ISSUES = List.of(
            "FREE_TERRAFORMING",                       // global parameter raised without TR for anybody - root cause not found yet
            "EXCEPTION_AssertionError:Negative plants" // FoodFactory / BiomassCombustors push plants below zero despite the validator
    );

    static final int GAMES_PER_CONFIG = Integer.getInteger("invariants.games", 15);

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    CardService cardService;

    @Test
    void invariantsHoldInEveryStepOfEveryGame() {
        Map<String, Integer> counts = new TreeMap<>();
        Map<String, String> examples = new TreeMap<>();

        List<GameParameters.GameParametersBuilder> configs = new ArrayList<>();
        for (PlayerDifficulty difficulty : List.of(PlayerDifficulty.RANDOM, PlayerDifficulty.SMART)) {
            configs.add(base(difficulty).dummyHand(true).mulligan(true).expansion(Expansion.BUFFED_CORPORATION));
            configs.add(base(difficulty).dummyHand(false).mulligan(false));
            configs.add(base(difficulty).dummyHand(true).mulligan(true).expansion(Expansion.INFRASTRUCTURE));
        }

        int games = 0;
        int steps = 0;
        for (GameParameters.GameParametersBuilder config : configs) {
            for (int g = 0; g < GAMES_PER_CONFIG; g++) {
                GameParameters parameters = config.build();
                MarsGame game = gameService.createNewSimulation(parameters);
                GameInvariants invariants = new GameInvariants(cardService);
                int[] stepCounter = {0};
                String label = parameters.getComputers().get(0) + "/" + parameters.getExpansions() + (parameters.isDummyHand() ? "/dummyHand" : "/noDummy") + (parameters.isMulligan() ? "/mulligan" : "");
                try {
                    simulation.processSimulation(game, current -> {
                        stepCounter[0]++;
                        if (stepCounter[0] > 20000) {
                            throw new IllegalStateException("Game does not finish (possible infinite loop)");
                        }
                        for (String v : invariants.check(current)) {
                            String key = v.substring(0, v.indexOf(' ')) + (parameters.isDummyHand() ? " [dummyHand]" : " [noDummy]");
                            counts.merge(key, 1, Integer::sum);
                            examples.putIfAbsent(key, v + "  [turn " + current.getTurns() + ", phase " + current.getCurrentPhase() + ", " + label + "]");
                        }
                    });
                } catch (RuntimeException | AssertionError e) {
                    String key = "EXCEPTION_" + e.getClass().getSimpleName() + ":" + e.getMessage();
                    counts.merge(key, 1, Integer::sum);
                    String trace = java.util.Arrays.stream(e.getStackTrace())
                            .filter(f -> f.getClassName().startsWith("com.terraforming"))
                            .limit(8).map(Object::toString)
                            .collect(java.util.stream.Collectors.joining(" <- "));
                    examples.putIfAbsent(key, e + " [" + label + "] " + trace);
                }
                games++;
                steps += stepCounter[0];
            }
        }

        System.out.println("=== INVARIANT REPORT: " + games + " games, " + steps + " steps, " + counts.size() + " distinct violation kinds ===");
        counts.forEach((k, c) -> System.out.println(k + " x" + c + "   e.g. " + examples.get(k)));
        List<String> unknown = counts.keySet().stream().filter(k -> KNOWN_ISSUES.stream().noneMatch(k::startsWith)).toList();
        assertTrue(unknown.isEmpty(), "New kinds of violations: " + unknown.stream().map(examples::get).toList());
    }

    private static GameParameters.GameParametersBuilder base(PlayerDifficulty difficulty) {
        return GameParameters.builder()
                .playerNames(List.of("A", "B"))
                .computers(List.of(difficulty, difficulty))
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY);
    }
}
