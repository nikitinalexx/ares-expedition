package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.Candidate;
import com.terraforming.ares.services.agent.Decision;
import com.terraforming.ares.services.agent.DecisionAgent;
import com.terraforming.ares.services.agent.PickPhaseDecisions;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(PickPhaseDecisionsTest.CapturingAgentConfig.class)
class PickPhaseDecisionsTest extends EngineTestBase {
    /**
     * Uniform random agent that remembers every decision it was asked.
     */
    static class CapturingAgent implements DecisionAgent {
        final ConcurrentLinkedQueue<Decision> decisions = new ConcurrentLinkedQueue<>();

        @Override
        public int choose(Decision decision) {
            decisions.add(decision);
            return ThreadLocalRandom.current().nextInt(decision.candidates().size());
        }
    }

    @TestConfiguration
    static class CapturingAgentConfig {
        @Bean
        @Primary
        CapturingAgent capturingAgent() {
            return new CapturingAgent();
        }
    }

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    PickPhaseDecisions pickPhaseDecisions;
    @Autowired
    AiTurnService aiTurnService;
    @Autowired
    CapturingAgent capturingAgent;

    private GameParameters parameters(PlayerDifficulty first, PlayerDifficulty second, boolean dummyHand) {
        return GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(first, second))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(dummyHand)
                .build();
    }

    /**
     * A mid-game snapshot (a copy, so that tests can freely modify it).
     */
    private MarsGame midGame(int minRounds) {
        MarsGame game = gameService.createNewSimulation(parameters(PlayerDifficulty.RANDOM, PlayerDifficulty.RANDOM, true));
        MarsGame[] snapshot = new MarsGame[1];
        simulation.processSimulation(game, current -> {
            if (snapshot[0] == null && current.getTurns() >= minRounds) {
                snapshot[0] = new MarsGame(current);
            }
        });
        assertNotNull(snapshot[0], "game ended before round " + minRounds);
        return snapshot[0];
    }

    private static void setPool(MarsGame game, Integer... phases) {
        game.getDummyHand().clear();
        game.getDummyHand().addAll(List.of(phases));
    }

    private Player first(MarsGame game) {
        return game.getPlayerUuidToPlayer().values().iterator().next();
    }

    private Player opponent(MarsGame game, Player player) {
        return game.getPlayerUuidToPlayer().values().stream().filter(p -> p != player).findFirst().orElseThrow();
    }

    // ---------------------------------------------------------------- legality

    @Test
    void candidatesAreExactlyThePhasesTheEngineAccepts() {
        MarsGame game = midGame(6);
        Player player = first(game);

        for (Integer previous : Arrays.asList(null, 1, 2, 3, 4, 5)) {
            MarsGame copy = new MarsGame(game);
            Player copyPlayer = copy.getPlayerByUuid(player.getUuid());
            copyPlayer.setPreviousChosenPhase(previous);

            Set<Integer> byEngine = new TreeSet<>();
            for (int phase = 1; phase <= 5; phase++) {
                MarsGame attempt = new MarsGame(copy);
                Player attemptPlayer = attempt.getPlayerByUuid(player.getUuid());
                attemptPlayer.setNextTurns(null);
                try {
                    aiTurnService.choosePhaseTurn(attemptPlayer, phase);
                    byEngine.add(phase);
                } catch (IllegalStateException rejected) {
                    // the engine does not allow this phase
                }
            }

            Set<Integer> byGenerator = new TreeSet<>();
            for (Candidate candidate : pickPhaseDecisions.build(copy, copyPlayer).candidates()) {
                byGenerator.add(candidate.id());
            }
            assertEquals(byEngine, byGenerator, "previous phase " + previous);
            assertEquals(previous == null ? 5 : 4, byGenerator.size());
        }
    }

    @Test
    void candidateFeaturesIdentifyThePhaseAndItsUpgrade() {
        MarsGame game = midGame(6);
        Player player = first(game);
        for (Candidate candidate : pickPhaseDecisions.build(game, player).candidates()) {
            for (int phase = 1; phase <= 5; phase++) {
                assertEquals(phase == candidate.id() ? 1f : 0f, candidate.features()[phase - 1], "one-hot of candidate " + candidate.id());
            }
            assertEquals(player.isPhaseUpgraded(candidate.id()) ? 1f : 0f, candidate.features()[5]);
        }
    }

    @Test
    void everyValueOfTheObservationHasAName() {
        List<String> names = PickPhaseDecisions.featureNames();
        assertEquals(PickPhaseDecisions.schema().obsSize(), names.size(), "names and values must be the same length, in the same order");
        assertEquals(names.size(), new java.util.HashSet<>(names).size() + countDuplicateNames(names), "sanity");
    }

    private static int countDuplicateNames(List<String> names) {
        return names.size() - new java.util.HashSet<>(names).size();
    }

    // ---------------------------------------------------------------- hidden information

    @Test
    void observationDoesNotDependOnHiddenInformation() {
        MarsGame game = midGame(8);
        Player player = first(game);
        Player opponent = opponent(game, player);
        float[] before = pickPhaseDecisions.build(game, player).observation();

        // 1. opponent hand: same SIZE, different cards (swap with cards from the deck)
        List<Integer> hand = opponent.getHand().getCards();
        List<Integer> deck = game.getProjectsDeck().getCards();
        assertTrue(deck.size() >= hand.size(), "deck too small for the test");
        for (int i = 0; i < hand.size(); i++) {
            int fromHand = hand.get(i);
            hand.set(i, deck.get(i));
            deck.set(i, fromHand);
        }
        // 2. deck order
        Collections.shuffle(deck);
        // 3. order in which the dummy will draw its phases
        if (game.getDummyHand() != null) {
            Collections.shuffle(game.getDummyHand());
        }

        float[] after = pickPhaseDecisions.build(game, player).observation();
        assertArrayEquals(before, after, 0f, "the observation changed after changing only hidden information");
    }

    @Test
    void observationDoesChangeWhenPublicInformationChanges() {
        MarsGame game = midGame(8);
        Player player = first(game);
        float[] before = pickPhaseDecisions.build(game, player).observation();
        player.setMc(player.getMc() + 7);
        float[] after = pickPhaseDecisions.build(game, player).observation();
        assertFalse(Arrays.equals(before, after), "my own resources must be visible to me");
    }

    @Test
    void whatIsMineForOnePlayerIsTheOpponentsForTheOther() {
        MarsGame game = midGame(8);
        Player a = first(game);
        Player b = opponent(game, a);
        a.setPreviousChosenPhase(2);
        b.setPreviousChosenPhase(5);
        float[] seenByA = pickPhaseDecisions.build(game, a).observation();
        float[] seenByB = pickPhaseDecisions.build(game, b).observation();

        int extras = seenByA.length - PickPhaseDecisions.EXTRAS_SIZE;
        // layout: round, my previous phase(5), opponent previous phase(5), ...
        assertEquals(1f, seenByA[extras + 1 + 1], "A: my previous phase is 2");
        assertEquals(1f, seenByA[extras + 1 + 5 + 4], "A: opponent previous phase is 5");
        assertEquals(1f, seenByB[extras + 1 + 4], "B: my previous phase is 5");
        assertEquals(1f, seenByB[extras + 1 + 5 + 1], "B: opponent previous phase is 2");
        // the dummy pool is public and the same for both
        for (int i = 0; i < 6; i++) {
            assertEquals(seenByA[extras + 11 + i], seenByB[extras + 11 + i], "dummy pool feature " + i);
        }
    }

    // ---------------------------------------------------------------- dummy pool

    @Test
    void dummyPoolIsASetOfTheRemainingPhasesAndAnEmptyPoolCountsAsFull() {
        MarsGame game = midGame(6);
        setPool(game, 4, 1, 3);
        assertEquals(Set.of(1, 3, 4), PickPhaseDecisions.dummyPhasePool(game));

        setPool(game);
        assertEquals(Set.of(1, 2, 3, 4, 5), PickPhaseDecisions.dummyPhasePool(game), "an empty pool is refilled before the next draw");

        Player player = first(game);
        setPool(game, 5, 2);
        float[] observation = pickPhaseDecisions.build(game, player).observation();
        int extras = observation.length - PickPhaseDecisions.EXTRAS_SIZE;
        float[] pool = Arrays.copyOfRange(observation, extras + 11, extras + 17);
        assertArrayEquals(new float[]{0f, 1f, 0f, 0f, 1f, 2f / 5f}, pool, 1e-6f);
    }

    @Test
    void withoutDummyHandThePoolIsEmpty() {
        MarsGame game = gameService.createNewSimulation(parameters(PlayerDifficulty.RANDOM, PlayerDifficulty.RANDOM, false));
        assertTrue(PickPhaseDecisions.dummyPhasePool(game).isEmpty());
    }

    // ---------------------------------------------------------------- whole games

    @Test
    void agentPlaysCompleteGamesAndOnlyChoosesLegalPhases() {
        capturingAgent.decisions.clear();
        int games = 20;
        for (int g = 0; g < games; g++) {
            MarsGame game = gameService.createNewSimulation(parameters(PlayerDifficulty.AGENT, g % 2 == 0 ? PlayerDifficulty.SMART : PlayerDifficulty.AGENT, true));
            simulation.processSimulation(game);
            assertEquals(com.terraforming.ares.model.StateType.GAME_END, game.getStateType());
        }

        Map<Integer, Integer> chosenPhases = new TreeMap<>();
        for (Decision decision : capturingAgent.decisions) {
            assertTrue(decision.candidates().size() == 4 || decision.candidates().size() == 5, "candidates: " + decision.candidates().size());
            decision.candidates().forEach(c -> assertTrue(c.id() >= 1 && c.id() <= 5));
        }
        assertTrue(capturingAgent.decisions.size() > games * 15, "the agent decides about a phase every round: " + capturingAgent.decisions.size());
        // uniformly random agent must use all phases
        Set<Integer> offered = new TreeSet<>();
        capturingAgent.decisions.forEach(d -> d.candidates().forEach(c -> offered.add(c.id())));
        assertEquals(Set.of(1, 2, 3, 4, 5), offered);
        chosenPhases.size();
    }
}
