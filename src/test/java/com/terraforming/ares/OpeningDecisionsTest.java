package com.terraforming.ares;

import com.terraforming.ares.cards.corporations.ApolloIndustriesCorporation;
import com.terraforming.ares.cards.corporations.AustellarCorporation;
import com.terraforming.ares.cards.corporations.ExocorpCorporation;
import com.terraforming.ares.cards.corporations.HyperionSystemsCorporation;
import com.terraforming.ares.cards.corporations.ModproCorporation;
import com.terraforming.ares.cards.corporations.NebuLabsCorporation;
import com.terraforming.ares.cards.corporations.SultiraCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.turn.CorporationChoiceTurn;
import com.terraforming.ares.processors.turn.PickCorporationProcessor;
import com.terraforming.ares.services.CardFactory;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.agent.Candidate;
import com.terraforming.ares.services.agent.CardSignature;
import com.terraforming.ares.services.agent.CorporationChoice;
import com.terraforming.ares.services.agent.CorporationDecisions;
import com.terraforming.ares.services.agent.Decision;
import com.terraforming.ares.services.agent.DecisionAgent;
import com.terraforming.ares.services.agent.DecisionFile;
import com.terraforming.ares.services.agent.DecisionLogs;
import com.terraforming.ares.services.agent.DecisionType;
import com.terraforming.ares.services.agent.MulliganDecisions;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Corporation and mulligan: the generated moves are exactly the ones the engine accepts, they carry no hidden
 * information, and a random agent that plays them (plus the phase decision) finishes games whose recorded labels
 * belong to the player who decided.
 */
@Import(OpeningDecisionsTest.CapturingAgentConfig.class)
class OpeningDecisionsTest extends EngineTestBase {
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

    @TempDir
    Path dir;

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    CorporationDecisions corporationDecisions;
    @Autowired
    MulliganDecisions mulliganDecisions;
    @Autowired
    CardFactory cardFactory;
    @Autowired
    CardValidationService cardValidationService;
    @Autowired
    PickCorporationProcessor pickCorporationProcessor;
    @Autowired
    AiTurnService aiTurnService;
    @Autowired
    DecisionLogs decisionLogs;
    @Autowired
    WinPointsService winPointsService;
    @Autowired
    CapturingAgent capturingAgent;

    private GameParameters parameters(PlayerDifficulty first, PlayerDifficulty second) {
        return GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(first, second))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true)
                .build();
    }

    private MarsGame opening() {
        return gameService.createNewSimulation(parameters(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT));
    }

    private static Player named(MarsGame game, String name) {
        return game.getPlayerUuidToPlayer().values().stream().filter(p -> p.getName().equals(name)).findFirst().orElseThrow();
    }

    private static void offer(Player player, int... corporationIds) {
        player.getCorporations().getCards().clear();
        for (int id : corporationIds) {
            player.getCorporations().addCard(id);
        }
    }

    private List<Card> everyCorporation() {
        List<Card> cards = new ArrayList<>();
        cards.addAll(cardFactory.createBaseCorporations().values());
        cards.addAll(cardFactory.createBuffedCorporations().values());
        cards.addAll(cardFactory.createDiscoveryCorporations().values());
        return cards;
    }

    // ---------------------------------------------------------------- corporation moves

    @Test
    void everyCorporationInputTheValidatorAcceptsIsGeneratedAndNothingElse() {
        MarsGame game = opening();
        Player player = named(game, "A");
        Set<Class<?>> withInput = new HashSet<>();
        for (Card corporation : everyCorporation()) {
            if (corporation.onBuiltEffectApplicableToItself()) {
                withInput.add(corporation.getClass());
            }
            assertEquals(minimalInputs(game, player, corporation), generatedInputs(corporation.getId(), corporationDecisions.legalInputs(game, player, corporation)),
                    corporation.getClass().getSimpleName());
        }
        assertEquals(Set.of(AustellarCorporation.class, ModproCorporation.class, NebuLabsCorporation.class, SultiraCorporation.class,
                ApolloIndustriesCorporation.class, HyperionSystemsCorporation.class, ExocorpCorporation.class), withInput);
    }

    @Test
    void everyGeneratedCorporationChoiceIsPlayedByTheEngine() {
        MarsGame game = opening();
        Player player = named(game, "A");
        int played = 0;
        for (Card corporation : everyCorporation()) {
            for (Map<Integer, List<Integer>> input : corporationDecisions.legalInputs(game, player, corporation)) {
                MarsGame copy = new MarsGame(game);
                pickCorporationProcessor.processTurn(new CorporationChoiceTurn(player.getUuid(), corporation.getId(), input), copy);
                Player copyPlayer = copy.getPlayerByUuid(player.getUuid());
                assertEquals(corporation.getId(), copyPlayer.getSelectedCorporationCard());
                if (corporation instanceof AustellarCorporation) {
                    assertEquals(input.get(InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId()).get(0), copyPlayer.getAustellarMilestone());
                    Tag tag = Tag.byIndex(input.get(InputFlag.TAG_INPUT.getId()).get(0));
                    assertEquals(List.of(tag), copyPlayer.getCardToTag().get(AustellarCorporation.class));
                }
                played++;
            }
        }
        assertTrue(played > everyCorporation().size(), "corporations with an input contribute more than one option");
    }

    @Test
    void corporationFeaturesDescribeTheCardAndTheInput() {
        MarsGame game = opening();
        Player player = named(game, "A");
        Card austellar = cardFactory.createDiscoveryCorporations().values().stream()
                .filter(c -> c instanceof AustellarCorporation).findFirst().orElseThrow();
        Card helion = cardFactory.createBaseCorporations().values().stream()
                .filter(c -> !c.onBuiltEffectApplicableToItself()).findFirst().orElseThrow();
        offer(player, helion.getId(), austellar.getId());

        List<CorporationChoice> choices = corporationDecisions.options(game, player);
        assertTrue(choices.size() > 2);
        Set<String> signatures = new HashSet<>();
        for (CorporationChoice choice : choices) {
            float[] features = choice.candidate().features();
            assertEquals(CorporationDecisions.CANDIDATE_FEATURES, features.length);
            assertEquals(choice.corporationId(), choice.candidate().id());
            float[] signature = Arrays.copyOf(features, CardSignature.BITS);
            signatures.add(Arrays.toString(signature));
            for (int i = 0; i < CardSignature.BITS; i++) {
                assertTrue(features[i] == 1f || features[i] == -1f);
            }
            int phaseOnes = count(features, CardSignature.BITS, CorporationDecisions.PHASE_UPGRADES);
            assertEquals(choice.input().containsKey(InputFlag.PHASE_UPGRADE_CARD.getId()) ? 1 : 0, phaseOnes);
            if (choice.input().containsKey(InputFlag.TAG_INPUT.getId())) {
                assertEquals(1, count(features, CardSignature.BITS + CorporationDecisions.PHASE_UPGRADES, CorporationDecisions.TAGS));
            }
            if (choice.input().containsKey(InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId())) {
                int slotAt = CardSignature.BITS + CorporationDecisions.PHASE_UPGRADES + CorporationDecisions.TAGS;
                assertEquals(1, count(features, slotAt, CorporationDecisions.MILESTONE_SLOTS));
                assertEquals(1, count(features, slotAt + CorporationDecisions.MILESTONE_SLOTS, CorporationDecisions.MILESTONE_TYPES));
            }
        }
        assertEquals(2, signatures.size(), "the two corporations must not share a signature");
    }

    // ---------------------------------------------------------------- mulligan moves

    @Test
    void everySubsetOfTheHandIsReachableAndAcceptedAndNothingOutsideTheHandIsOffered() {
        MarsGame game = opening();
        Player player = named(game, "A");
        List<Integer> hand = new ArrayList<>(player.getHand().getCards());
        assertEquals(com.terraforming.ares.model.Constants.DEFAULT_START_HAND_SIZE, hand.size());

        Decision first = mulliganDecisions.build(game, player, List.of());
        assertEquals(new TreeSet<>(hand), idsExceptStop(first));
        assertEquals(MulliganDecisions.STOP, first.candidates().getLast().id());
        assertEquals(1f, first.candidates().getLast().features()[MulliganDecisions.CANDIDATE_FEATURES - 1]);

        int outside = game.getProjectsDeck().getCards().getFirst();
        assertFalse(idsExceptStop(first).contains(outside));

        for (int mask = 0; mask < (1 << hand.size()); mask++) {
            List<Integer> chosen = new ArrayList<>();
            for (int i = 0; i < hand.size(); i++) {
                if ((mask & (1 << i)) != 0) {
                    chosen.add(hand.get(i));
                }
            }
            List<Integer> soFar = new ArrayList<>();
            for (int card : chosen) {
                Decision step = mulliganDecisions.build(game, player, soFar);
                assertTrue(idsExceptStop(step).contains(card), "card " + card + " dropped while discarding " + soFar);
                assertEquals(MulliganDecisions.STOP, step.candidates().getLast().id());
                soFar.add(card);
            }
            Decision after = mulliganDecisions.build(game, player, chosen);
            for (int card : chosen) {
                int stillThere = Collections.frequency(hand, card) - Collections.frequency(chosen, card);
                assertTrue(countId(after, card) <= stillThere, "discarded card " + card + " is still offered");
            }

            MarsGame copy = new MarsGame(game);
            aiTurnService.mulliganCards(copy, copy.getPlayerByUuid(player.getUuid()), new ArrayList<>(chosen));
            assertEquals(hand.size(), copy.getPlayerByUuid(player.getUuid()).getHand().size(), "mulligan replaces what it discards");
            assertFalse(copy.getPlayerByUuid(player.getUuid()).isMulligan());
        }
    }

    @Test
    void mulliganCandidatesDoNotDependOnTheOrderOfTheHand() {
        MarsGame game = opening();
        Player player = named(game, "A");
        List<Candidate> before = mulliganDecisions.build(game, player, List.of()).candidates();
        Collections.shuffle(player.getHand().getCards());
        List<Candidate> after = mulliganDecisions.build(game, player, List.of()).candidates();
        assertEquals(before.size(), after.size());
        for (int i = 0; i < before.size(); i++) {
            assertEquals(before.get(i).id(), after.get(i).id());
            assertArrayEquals(before.get(i).features(), after.get(i).features());
        }
    }

    @Test
    void mulliganFeaturesIdentifyTheCardAndTheStop() {
        MarsGame game = opening();
        Player player = named(game, "A");
        Decision decision = mulliganDecisions.build(game, player, List.of());
        Set<String> signatures = new HashSet<>();
        for (Candidate candidate : decision.candidates()) {
            assertEquals(MulliganDecisions.CANDIDATE_FEATURES, candidate.features().length);
            boolean stop = candidate.id() == MulliganDecisions.STOP;
            assertEquals(stop ? 1f : 0f, candidate.features()[candidate.features().length - 1]);
            if (!stop) {
                for (int i = 0; i < CardSignature.BITS; i++) {
                    assertTrue(candidate.features()[i] == 1f || candidate.features()[i] == -1f);
                }
                signatures.add(Arrays.toString(Arrays.copyOf(candidate.features(), CardSignature.BITS)));
            } else {
                for (int i = 0; i < candidate.features().length - 1; i++) {
                    assertEquals(0f, candidate.features()[i]);
                }
            }
        }
        assertTrue(signatures.size() >= 2);
    }

    // ---------------------------------------------------------------- hidden information and catalog

    @Test
    void openingObservationIgnoresHiddenInformationAndSeesMyOwnHand() {
        MarsGame game = opening();
        Player player = named(game, "A");
        Player opponent = named(game, "B");
        float[] corporation = observationOf(corporationDecisions, game, player);
        float[] mulligan = mulliganDecisions.build(game, player, List.of()).observation();

        List<Integer> hand = opponent.getHand().getCards();
        List<Integer> deck = game.getProjectsDeck().getCards();
        for (int i = 0; i < hand.size(); i++) {
            int fromHand = hand.get(i);
            hand.set(i, deck.get(i));
            deck.set(i, fromHand);
        }
        Collections.shuffle(deck);
        Collections.shuffle(game.getDummyHand());
        List<Integer> remaining = game.getCorporationsDeck().getCards();
        assertTrue(remaining.size() >= 2);
        opponent.getCorporations().getCards().clear();
        opponent.getCorporations().addCard(remaining.get(0));
        opponent.getCorporations().addCard(remaining.get(1));

        assertArrayEquals(corporation, observationOf(corporationDecisions, game, player));
        assertArrayEquals(mulligan, mulliganDecisions.build(game, player, List.of()).observation());

        MarsGame copy = new MarsGame(game);
        Player copyPlayer = copy.getPlayerByUuid(player.getUuid());
        int replacement = copy.getProjectsDeck().getCards().getFirst();
        assertNotEquals(replacement, copyPlayer.getHand().getCards().getFirst());
        copyPlayer.getHand().getCards().set(0, replacement);
        assertFalse(Arrays.equals(mulligan, mulliganDecisions.build(copy, copyPlayer, List.of()).observation()));
    }

    @Test
    void cardSignaturesAreUniqueAcrossTheCatalog() {
        List<Card> cards = new ArrayList<>(everyCorporation());
        cards.addAll(cardFactory.getAllProjects(List.of(Expansion.BASE, Expansion.DISCOVERY, Expansion.INFRASTRUCTURE)));
        cards.addAll(cardFactory.getExperimentalProjects());
        Map<String, String> signatureToClass = new HashMap<>();
        Set<Class<?>> seen = new HashSet<>();
        for (Card card : cards) {
            if (!seen.add(card.getClass())) {
                continue;
            }
            String signature = Arrays.toString(CardSignature.of(card.getClass()));
            String other = signatureToClass.put(signature, card.getClass().getName());
            assertNull(other, "same signature for " + other + " and " + card.getClass().getName());
        }
        assertTrue(seen.size() > 200, "the catalog under test is too small: " + seen.size());
    }

    // ---------------------------------------------------------------- whole games and labels

    @Test
    void randomAgentFinishesGamesUsingOnlyTheseDecisions() {
        capturingAgent.decisions.clear();
        for (int g = 0; g < 6; g++) {
            PlayerDifficulty opponent = g % 2 == 0 ? PlayerDifficulty.SMART : PlayerDifficulty.AGENT;
            MarsGame game = gameService.createNewSimulation(parameters(PlayerDifficulty.AGENT, opponent));
            simulation.processSimulation(game);
            assertEquals(StateType.GAME_END, game.getStateType());
        }
        int corporations = 0;
        int mulligans = 0;
        for (Decision decision : capturingAgent.decisions) {
            if (decision.type() == DecisionType.PICK_CORPORATION) {
                corporations++;
                assertTrue(decision.candidates().size() >= 2);
                decision.candidates().forEach(c -> assertTrue(c.id() >= 10000 && c.id() < 20000));
            }
            if (decision.type() == DecisionType.MULLIGAN) {
                mulligans++;
                assertEquals(MulliganDecisions.STOP, decision.candidates().getLast().id());
                assertTrue(decision.candidates().size() >= 1 && decision.candidates().size() <= MulliganDecisions.MAX_CANDIDATES);
            }
        }
        assertTrue(corporations >= 6, "every agent founds a corporation: " + corporations);
        assertTrue(mulligans >= 6, "every agent resolves the mulligan: " + mulligans);
    }

    @Test
    void recordedOpeningDecisionsAreLabelledForThePlayerWhoMadeThem() throws Exception {
        decisionLogs.startRecording(dir);
        Map<Long, float[]> expected = new HashMap<>();
        try {
            for (int g = 0; g < 4; g++) {
                MarsGame game = gameService.createNewSimulation(parameters(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT));
                simulation.processSimulation(game);
                int pointsA = winPointsService.countWinPoints(named(game, "A"), game);
                int pointsB = winPointsService.countWinPoints(named(game, "B"), game);
                // finish() numbers games from 0 in the order they are finished; seat 0 is the player named A
                expected.put((long) g, new float[]{outcome(pointsA, pointsB), pointsA - pointsB, outcome(pointsB, pointsA), pointsB - pointsA});
                decisionLogs.finish(game);
            }
        } finally {
            decisionLogs.stopRecording();
        }

        DecisionFile.Content corporations = readChunks(dir, "PICK_CORPORATION");
        DecisionFile.Content mulligans = readChunks(dir, "MULLIGAN");
        assertEquals(CorporationDecisions.schema(), corporations.schema());
        assertEquals(MulliganDecisions.schema(), mulligans.schema());
        assertFalse(corporations.rows().isEmpty());
        assertFalse(mulligans.rows().isEmpty());

        Map<Long, Map<Integer, Integer>> mulliganStep = new HashMap<>();
        Map<Long, Map<Integer, Integer>> corporationStep = new HashMap<>();
        checkRows(corporations.rows(), expected, corporationStep, true);
        checkRows(mulligans.rows(), expected, mulliganStep, false);
        assertEquals(expected.keySet(), corporationStep.keySet());
        for (long gameId : expected.keySet()) {
            assertTrue(corporationStep.get(gameId).get(0) > mulliganStep.get(gameId).get(0));
            assertTrue(corporationStep.get(gameId).get(1) > mulliganStep.get(gameId).get(1));
        }
    }

    private static float outcome(int mine, int theirs) {
        return mine > theirs ? 1f : (mine == theirs ? 0.5f : 0f);
    }

    // ---------------------------------------------------------------- helpers

    private float[] observationOf(CorporationDecisions decisions, MarsGame game, Player player) {
        return decisions.decision(game, player, decisions.options(game, player)).observation();
    }

    private Set<String> generatedInputs(int corporationId, List<Map<Integer, List<Integer>>> inputs) {
        Set<String> keys = new TreeSet<>();
        for (Map<Integer, List<Integer>> input : inputs) {
            keys.add(key(corporationId, input));
        }
        return keys;
    }

    /**
     * Inputs from a grid written independently of the generator. An input the validator accepts is kept only when no
     * smaller accepted input is the same choice with extra ignored keys (otherwise every corporation would "accept"
     * junk keys it never reads).
     */
    private Set<String> minimalInputs(MarsGame game, Player player, Card card) {
        int phase = InputFlag.PHASE_UPGRADE_CARD.getId();
        int tag = InputFlag.TAG_INPUT.getId();
        int milestone = InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId();
        List<Map<Integer, List<Integer>>> grid = new ArrayList<>();
        grid.add(Map.of());
        for (int upgrade = -1; upgrade <= 10; upgrade++) {
            grid.add(Map.of(phase, List.of(upgrade)));
        }
        for (int value = -1; value <= Tag.values().length; value++) {
            grid.add(Map.of(tag, List.of(value)));
        }
        for (int slot = -1; slot <= game.getMilestones().size(); slot++) {
            grid.add(Map.of(milestone, List.of(slot)));
        }
        for (int value = 0; value < Tag.values().length; value++) {
            for (int slot = 0; slot < game.getMilestones().size(); slot++) {
                grid.add(Map.of(tag, List.of(value), milestone, List.of(slot)));
            }
        }
        for (int upgrade = 0; upgrade <= 9; upgrade++) {
            grid.add(Map.of(phase, List.of(upgrade), tag, List.of(Tag.SCIENCE.ordinal())));
        }

        List<Map<Integer, List<Integer>>> passing = new ArrayList<>();
        for (Map<Integer, List<Integer>> input : grid) {
            if (cardValidationService.validateCorporation(player, game, card.getId(), input) == null) {
                passing.add(input);
            }
        }
        Set<String> minimal = new TreeSet<>();
        for (Map<Integer, List<Integer>> input : passing) {
            boolean biggerThanAnother = false;
            for (Map<Integer, List<Integer>> other : passing) {
                if (isStrictSubmap(other, input)) {
                    biggerThanAnother = true;
                    break;
                }
            }
            if (!biggerThanAnother) {
                minimal.add(key(card.getId(), input));
            }
        }
        return minimal;
    }

    private static boolean isStrictSubmap(Map<Integer, List<Integer>> small, Map<Integer, List<Integer>> big) {
        if (small.size() >= big.size()) {
            return false;
        }
        for (var entry : small.entrySet()) {
            if (!entry.getValue().equals(big.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static String key(int corporationId, Map<Integer, List<Integer>> input) {
        return corporationId + "=" + new TreeMap<>(input);
    }

    private static int count(float[] features, int from, int length) {
        int ones = 0;
        for (int i = from; i < from + length; i++) {
            if (features[i] != 0f) {
                ones++;
            }
        }
        return ones;
    }

    private static Set<Integer> idsExceptStop(Decision decision) {
        Set<Integer> ids = new TreeSet<>();
        for (Candidate candidate : decision.candidates()) {
            if (candidate.id() != MulliganDecisions.STOP) {
                ids.add(candidate.id());
            }
        }
        return ids;
    }

    private static int countId(Decision decision, int id) {
        int n = 0;
        for (Candidate candidate : decision.candidates()) {
            if (candidate.id() == id) {
                n++;
            }
        }
        return n;
    }

    private static DecisionFile.Content readChunks(Path directory, String type) throws Exception {
        List<DecisionFile.Row> rows = new ArrayList<>();
        DecisionFile.Content first = null;
        try (var files = java.nio.file.Files.list(directory)) {
            for (Path file : files.filter(f -> f.getFileName().toString().matches(type + "\\.\\d+\\.ares")).sorted().toList()) {
                DecisionFile.Content content = DecisionFile.read(file);
                if (first == null) {
                    first = content;
                }
                assertEquals(first.schema(), content.schema());
                rows.addAll(content.rows());
            }
        }
        return new DecisionFile.Content(first.schema(), first.schemaJson(), rows);
    }

    private static void checkRows(List<DecisionFile.Row> rows, Map<Long, float[]> expected,
                                  Map<Long, Map<Integer, Integer>> stepBySeat, boolean corporation) {
        for (DecisionFile.Row row : rows) {
            assertTrue(row.chosen() < row.candidateCount());
            assertEquals(0, row.context(), "AGENT records the SMART context");
            float[] result = expected.get(row.gameId());
            assertTrue(result != null, "unexpected game " + row.gameId());
            if (row.seat() == 0) {
                assertEquals(result[0], row.outcome(), 0f);
                assertEquals(result[1], row.margin(), 0f);
            } else {
                assertEquals(1, row.seat());
                assertEquals(result[2], row.outcome(), 0f);
                assertEquals(result[3], row.margin(), 0f);
            }
            for (int i = 0; i < row.candidateCount(); i++) {
                int id = row.candidateIds()[i];
                if (corporation) {
                    assertTrue(id >= 10000 && id < 20000, "corporation id " + id);
                } else {
                    assertTrue(id == MulliganDecisions.STOP || (id > 0 && id < 20000), "mulligan id " + id);
                }
            }
            stepBySeat.computeIfAbsent(row.gameId(), k -> new HashMap<>()).merge(row.seat(), row.step(), Math::max);
        }
    }
}
