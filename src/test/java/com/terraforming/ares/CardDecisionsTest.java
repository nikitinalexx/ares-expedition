package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.SellCardsLastRoundTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.processors.turn.SellCardsLastRoundTurnProcessor;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.agent.CardFeatures;
import com.terraforming.ares.services.agent.DecisionFile;
import com.terraforming.ares.services.agent.DecisionLogs;
import com.terraforming.ares.services.agent.DecisionType;
import com.terraforming.ares.services.agent.HandCardDecisions;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Draft, discard and both sales: the generated cards are the ones the engine accepts, a sale pays the sell price and
 * leaves the build in place, and a random agent finishes games.
 */
class CardDecisionsTest extends EngineTestBase {
    @TempDir
    Path dir;

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    CardService cardService;
    @Autowired
    MarsContextProvider marsContextProvider;
    @Autowired
    HandCardDecisions handCardDecisions;
    @Autowired
    AiTurnService aiTurnService;
    @Autowired
    SellCardsLastRoundTurnProcessor sellCardsLastRoundTurnProcessor;
    @Autowired
    SpecialEffectsService specialEffectsService;
    @Autowired
    DecisionLogs decisionLogs;
    @Autowired
    WinPointsService winPointsService;

    @Test
    void discardingExactlyTheRequiredNumberReachesEverySubsetAndTheEngineAcceptsIt() {
        MarsGame game = prepared();
        Player player = named(game, "A");
        List<Integer> hand = giveHand(game, player, 5);
        int need = 2;

        Set<String> reachable = new TreeSet<>();
        walk(game, player, DecisionType.DISCARD_CARDS, hand, new ArrayList<>(), need, false, reachable);
        assertEquals(combinations(hand.size(), need), reachable.size());

        for (String key : reachable) {
            List<Integer> subset = parse(key);
            MarsGame copy = new MarsGame(game);
            Player copyPlayer = copy.getPlayerByUuid(player.getUuid());
            clearTurns(copyPlayer);
            copyPlayer.addNextTurn(new DiscardCardsTurn(copyPlayer.getUuid(), List.of(), need, false, true, List.of()));
            aiTurnService.discardCards(copy, copyPlayer,
                    new DiscardCardsTurn(copyPlayer.getUuid(), subset, subset.size(), false, false, List.of()), subset);
            for (int id : subset) {
                assertFalse(copyPlayer.getHand().getCards().contains(id), "discarded " + id);
            }
            assertEquals(hand.size() - need, copyPlayer.getHand().size());
        }

        clearTurns(player);
        player.addNextTurn(new DiscardCardsTurn(player.getUuid(), List.of(), need, false, true, List.of()));
        assertThrows(IllegalStateException.class, () -> aiTurnService.discardCards(game, player,
                new DiscardCardsTurn(player.getUuid(), List.of(hand.getFirst()), 1, false, false, List.of()),
                List.of(hand.getFirst())));
    }

    @Test
    void aDraftOffersOnlyTheRevealedCards() {
        MarsGame game = prepared();
        Player player = named(game, "A");
        List<Integer> hand = giveHand(game, player, 6);
        List<Integer> revealed = List.of(hand.get(0), hand.get(2), hand.get(4));

        var decision = handCardDecisions.build(DecisionType.DRAFT_CARDS, game, player, revealed, List.of(), false);
        assertEquals(sorted(revealed), ids(decision));
        assertFalse(ids(decision).contains(hand.get(1)));
        assertEquals(CardFeatures.LENGTH, decision.candidates().getFirst().features().length);

        clearTurns(player);
        player.addNextTurn(new DiscardCardsTurn(player.getUuid(), revealed, 2, true, true, List.of()));
        List<Integer> keep = List.of(revealed.get(0), revealed.get(1));
        aiTurnService.discardCards(game, player,
                new DiscardCardsTurn(player.getUuid(), keep, keep.size(), false, false, List.of()), keep);
        assertTrue(player.getHand().getCards().contains(revealed.get(2)));
        assertFalse(player.getHand().getCards().contains(revealed.get(0)));

        MarsGame rejected = prepared();
        Player other = named(rejected, "A");
        List<Integer> otherHand = giveHand(rejected, other, 4);
        clearTurns(other);
        other.addNextTurn(new DiscardCardsTurn(other.getUuid(), new ArrayList<>(otherHand.subList(0, 3)), 1, true, true, List.of()));
        int outside = otherHand.get(3);
        assertThrows(IllegalStateException.class, () -> aiTurnService.discardCards(rejected, other,
                new DiscardCardsTurn(other.getUuid(), List.of(outside), 1, false, false, List.of()),
                List.of(outside)));
    }

    @Test
    void sellingPaysThreeEachAndDoesNotSpendTheBuild() {
        MarsGame game = prepared();
        Player player = named(game, "A");
        List<Integer> hand = giveHand(game, player, 4);
        assertEquals(3, specialEffectsService.getCardPrice(player));

        var first = handCardDecisions.build(DecisionType.SELL_CARDS, game, player, hand, List.of(), true);
        assertEquals(CardFeatures.STOP_ID, first.candidates().getLast().id());
        assertEquals(0.3f, first.candidates().getFirst().features()[CardFeatures.LENGTH]);
        assertEquals(0f, first.candidates().getLast().features()[CardFeatures.LENGTH]);
        assertEquals(1f, first.candidates().getLast().features()[CardFeatures.LENGTH - 1]);

        Set<String> reachable = new TreeSet<>();
        walk(game, player, DecisionType.SELL_CARDS, hand, new ArrayList<>(), 0, true, reachable);
        assertTrue(reachable.contains("[]"));
        assertEquals((1 << hand.size()), reachable.size(), "every subset, including selling nothing");

        List<TurnType> before = gameService.getPossibleTurns(game, player.getUuid());
        assertTrue(before.contains(TurnType.SELL_CARDS));
        assertTrue(before.contains(TurnType.BUILD_GREEN_PROJECT));
        assertTrue(before.size() > 1);

        int mc = player.getMc();
        int builds = player.getBuilds().size();
        aiTurnService.sellCards(player, game, List.of(hand.getFirst()));
        assertEquals(mc + 3, player.getMc());
        assertEquals(builds, player.getBuilds().size());
        assertTrue(player.canBuildGreen());
        List<TurnType> after = gameService.getPossibleTurns(game, player.getUuid());
        assertTrue(after.contains(TurnType.BUILD_GREEN_PROJECT), "the build is still there: " + after);
        assertTrue(after.contains(TurnType.SELL_CARDS));

        MarsGame blue = prepared();
        blue.setStateType(StateType.BUILD_BLUE_RED_PROJECTS, marsContextProvider.provide(blue));
        Player bluePlayer = named(blue, "A");
        clearTurns(bluePlayer);
        giveHand(blue, bluePlayer, 2);
        bluePlayer.getBuilds().add(new BuildDto(BuildType.BLUE_RED));
        List<TurnType> phaseTwo = gameService.getPossibleTurns(blue, bluePlayer.getUuid());
        assertTrue(phaseTwo.contains(TurnType.SELL_CARDS) && phaseTwo.contains(TurnType.BUILD_BLUE_RED_PROJECT), phaseTwo.toString());
    }

    @Test
    void theLastRoundSaleMustReachTheHandLimitAndMaySellMore() {
        MarsGame game = prepared();
        Player player = named(game, "A");
        List<Integer> hand = giveHand(game, player, 12);

        var first = handCardDecisions.build(DecisionType.SELL_CARDS_LAST_ROUND, game, player, hand, List.of(), false);
        assertEquals(hand.size(), first.candidates().size());
        assertFalse(ids(first).contains(CardFeatures.STOP_ID));

        List<Integer> two = List.of(hand.get(0), hand.get(1));
        var afterMinimum = handCardDecisions.build(DecisionType.SELL_CARDS_LAST_ROUND, game, player, hand, two, true);
        assertEquals(CardFeatures.STOP_ID, afterMinimum.candidates().getLast().id());
        assertEquals(hand.size() - 2 + 1, afterMinimum.candidates().size());

        clearTurns(player);
        assertThrows(IllegalStateException.class,
                () -> aiTurnService.sellCardsLastRoundTurn(player, List.of(hand.getFirst())));

        MarsGame kept = new MarsGame(game);
        Player keptPlayer = kept.getPlayerByUuid(player.getUuid());
        sellCardsLastRoundTurnProcessor.processTurn(new SellCardsLastRoundTurn(keptPlayer.getUuid(), two), kept);
        assertEquals(10, keptPlayer.getHand().size());
        assertEquals(6, keptPlayer.getMc());

        MarsGame emptied = new MarsGame(game);
        Player emptiedPlayer = emptied.getPlayerByUuid(player.getUuid());
        sellCardsLastRoundTurnProcessor.processTurn(new SellCardsLastRoundTurn(emptiedPlayer.getUuid(), hand), emptied);
        assertEquals(0, emptiedPlayer.getHand().size());
        assertEquals(36, emptiedPlayer.getMc());
    }

    @Test
    void theHandOrderAndHiddenInformationDoNotChangeTheDecision() {
        MarsGame game = prepared();
        Player player = named(game, "A");
        Player opponent = named(game, "B");
        List<Integer> hand = giveHand(game, player, 5);
        giveHand(game, opponent, 5);
        float[] seen = handCardDecisions.build(DecisionType.SELL_CARDS, game, player, hand, List.of(), true).observation();

        Collections.shuffle(player.getHand().getCards());
        var reordered = handCardDecisions.build(DecisionType.SELL_CARDS, game, player, player.getHand().getCards(), List.of(), true);
        assertEquals(sorted(hand), cardIds(reordered));

        Collections.shuffle(opponent.getHand().getCards());
        Collections.shuffle(game.getProjectsDeck().getCards());
        assertTrue(Arrays.equals(seen, handCardDecisions.build(
                DecisionType.SELL_CARDS, game, player, player.getHand().getCards(), List.of(), true).observation()));

        int replacement = cardService.dealCards(game, 1).getFirst();
        player.getHand().getCards().set(0, replacement);
        assertFalse(ids(handCardDecisions.build(
                DecisionType.SELL_CARDS, game, player, player.getHand().getCards(), List.of(), true)).containsAll(sorted(hand)));
    }

    @Test
    void aRandomAgentFinishesGamesAndTheSaleIsLabelledForWhoeverSold() throws Exception {
        decisionLogs.startRecording(dir);
        try {
            for (int g = 0; g < 3; g++) {
                MarsGame game = gameService.createNewSimulation(parameters());
                simulation.processSimulation(game);
                assertEquals(StateType.GAME_END, game.getStateType());
                decisionLogs.finish(game);
            }
        } finally {
            decisionLogs.stopRecording();
        }

        int sales = 0;
        try (var files = java.nio.file.Files.list(dir)) {
            for (Path file : files.filter(f -> f.getFileName().toString().startsWith("SELL_CARDS.")).sorted().toList()) {
                DecisionFile.Content content = DecisionFile.read(file);
                assertEquals(DecisionType.SELL_CARDS, content.schema().type());
                assertEquals(HandCardDecisions.SELL_FEATURES, content.schema().candidateFeatures());
                for (DecisionFile.Row row : content.rows()) {
                    sales++;
                    assertTrue(row.chosen() < row.candidateCount());
                    assertEquals(0, row.context(), "both players are AGENT, so the fallback context is SMART");
                    boolean stopped = false;
                    for (int i = 0; i < row.candidateCount(); i++) {
                        int id = row.candidateIds()[i];
                        assertTrue(id == CardFeatures.STOP_ID || (id > 0 && id < 10000), "card id " + id);
                        if (id == CardFeatures.STOP_ID) {
                            stopped = true;
                        }
                    }
                    assertTrue(stopped, "a sale can always be declined");
                }
            }
        }
        assertTrue(sales > 0, "an agent with a hand and another action is offered the sale");
    }

    private void walk(MarsGame game, Player player, DecisionType type, List<Integer> pool, List<Integer> chosen,
                      int minimum, boolean stopAllowed, Set<String> reachable) {
        boolean mayStop = stopAllowed && chosen.size() >= minimum;
        if (!stopAllowed && chosen.size() == minimum) {
            reachable.add(key(chosen));
            return;
        }
        var decision = handCardDecisions.build(type, game, player, pool, chosen, mayStop);
        int remaining = pool.size() - chosen.size();
        assertEquals(remaining + (mayStop ? 1 : 0), decision.candidates().size());
        for (var candidate : decision.candidates()) {
            if (candidate.id() == CardFeatures.STOP_ID) {
                assertTrue(mayStop);
                reachable.add(key(chosen));
                continue;
            }
            assertTrue(count(pool, candidate.id()) > count(chosen, candidate.id()));
            List<Integer> next = new ArrayList<>(chosen);
            next.add(candidate.id());
            walk(game, player, type, pool, next, minimum, stopAllowed, reachable);
        }
    }

    private MarsGame prepared() {
        MarsGame game = gameService.createNewSimulation(parameters());
        game.setStateType(StateType.BUILD_GREEN_PROJECTS, marsContextProvider.provide(game));
        Player player = named(game, "A");
        clearTurns(player);
        player.getBuilds().add(new BuildDto(BuildType.GREEN));
        player.setMc(0);
        return game;
    }

    private List<Integer> giveHand(MarsGame game, Player player, int count) {
        player.getHand().getCards().clear();
        List<Integer> cards = cardService.dealCards(game, count);
        player.getHand().addCards(cards);
        return new ArrayList<>(cards);
    }

    private static void clearTurns(Player player) {
        while (player.getNextTurn() != null) {
            player.removeNextTurn();
        }
    }

    private static GameParameters parameters() {
        return GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true)
                .build();
    }

    private static Player named(MarsGame game, String name) {
        return game.getPlayerUuidToPlayer().values().stream().filter(p -> p.getName().equals(name)).findFirst().orElseThrow();
    }

    private static List<Integer> cardIds(com.terraforming.ares.services.agent.Decision decision) {
        List<Integer> ids = new ArrayList<>();
        for (var candidate : decision.candidates()) {
            if (candidate.id() != CardFeatures.STOP_ID) {
                ids.add(candidate.id());
            }
        }
        return ids;
    }

    private static List<Integer> ids(com.terraforming.ares.services.agent.Decision decision) {
        List<Integer> ids = new ArrayList<>();
        for (var candidate : decision.candidates()) {
            ids.add(candidate.id());
        }
        return ids;
    }

    private static List<Integer> sorted(List<Integer> ids) {
        List<Integer> copy = new ArrayList<>(ids);
        copy.sort(Integer::compareTo);
        return copy;
    }

    private static String key(List<Integer> ids) {
        List<Integer> copy = sorted(ids);
        return copy.toString();
    }

    private static List<Integer> parse(String key) {
        if (key.equals("[]")) {
            return List.of();
        }
        String[] parts = key.substring(1, key.length() - 1).split(", ");
        List<Integer> ids = new ArrayList<>();
        for (String part : parts) {
            ids.add(Integer.parseInt(part));
        }
        return ids;
    }

    private static int count(List<Integer> ids, int id) {
        int n = 0;
        for (int value : ids) {
            if (value == id) {
                n++;
            }
        }
        return n;
    }

    private static int combinations(int n, int k) {
        int value = 1;
        for (int i = 0; i < k; i++) {
            value = value * (n - i) / (i + 1);
        }
        return value;
    }
}
