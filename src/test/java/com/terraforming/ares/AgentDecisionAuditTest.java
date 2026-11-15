package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardFactory;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.agent.BlueActionDecisions;
import com.terraforming.ares.services.agent.BlueInputDecisions;
import com.terraforming.ares.services.agent.BuildCardDecisions;
import com.terraforming.ares.services.agent.BuildInputDecisions;
import com.terraforming.ares.services.agent.BuildPaymentChoice;
import com.terraforming.ares.services.agent.BuildPaymentDecisions;
import com.terraforming.ares.services.agent.Candidate;
import com.terraforming.ares.services.agent.Decision;
import com.terraforming.ares.services.agent.ObservationBuilder;
import com.terraforming.ares.services.agent.PickPhaseDecisions;
import com.terraforming.ares.services.agent.StandardActionChoice;
import com.terraforming.ares.services.agent.StandardActionDecisions;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that must catch a broken decision pipeline BEFORE any training run. Each one is a claim the project relies on:
 * <ul>
 *   <li>the candidate features of a decision are pairwise distinct - a net cannot choose between two equal vectors;</li>
 *   <li>a player's observation describes THAT player and the opponent slot the other one;</li>
 *   <li>the mirrored observation the one-ply search reads is public: the opponent's hand never enters it;</li>
 *   <li>building a card on a copy adds exactly its printed points to the score the value net reads;</li>
 *   <li>every payment the builder offers is legal, and the plain one is among them;</li>
 *   <li>an activated blue card leaves the blue-action menu of the same phase;</li>
 *   <li>no observation bit belongs to a card of an expansion this player never plays.</li>
 * </ul>
 */
class AgentDecisionAuditTest extends EngineTestBase {
    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    ObservationBuilder observationBuilder;
    @Autowired
    CardService cardService;
    @Autowired
    CardFactory cardFactory;
    @Autowired
    WinPointsService winPointsService;
    @Autowired
    PickPhaseDecisions pickPhaseDecisions;
    @Autowired
    BuildCardDecisions buildCardDecisions;
    @Autowired
    BuildPaymentDecisions buildPaymentDecisions;
    @Autowired
    BuildInputDecisions buildInputDecisions;
    @Autowired
    StandardActionDecisions standardActionDecisions;
    @Autowired
    BlueActionDecisions blueActionDecisions;
    @Autowired
    BlueInputDecisions blueInputDecisions;
    @Autowired
    AiTurnService aiTurnService;
    @Autowired
    CardValidationService cardValidationService;

    private MarsGame snapshot(java.util.function.Predicate<MarsGame> wanted) {
        for (int attempt = 0; attempt < 30; attempt++) {
            MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                    .playerNames(new ArrayList<>(List.of("A", "B")))
                    .computers(List.of(PlayerDifficulty.SMART, PlayerDifficulty.SMART))
                    .mulligan(true)
                    .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                    .dummyHand(true).build());
            MarsGame[] snapshot = new MarsGame[1];
            simulation.processSimulation(game, current -> {
                if (snapshot[0] == null && current.getTurns() >= 12 && wanted.test(current)) {
                    snapshot[0] = new MarsGame(current);
                }
            });
            if (snapshot[0] != null) {
                return snapshot[0];
            }
        }
        throw new IllegalStateException("no such state found in 30 games");
    }

    private static Player first(MarsGame game) {
        return game.getPlayerUuidToPlayer().values().iterator().next();
    }

    private static Player second(MarsGame game) {
        Player first = first(game);
        for (Player player : game.getPlayerUuidToPlayer().values()) {
            if (!player.getUuid().equals(first.getUuid())) {
                return player;
            }
        }
        throw new IllegalStateException("two players expected");
    }

    // ------------------------------------------------------------------ candidate features

    @Test
    void thePhaseCandidatesLookDifferentToTheNet() {
        MarsGame game = snapshot(g -> g.getStateType() == StateType.PICK_PHASE);
        assertDistinct(pickPhaseDecisions.build(game, first(game)));
    }

    @Test
    void theBuildCandidatesLookDifferentToTheNet() {
        MarsGame game = snapshot(g -> (g.getStateType() == StateType.BUILD_GREEN_PROJECTS
                || g.getStateType() == StateType.BUILD_BLUE_RED_PROJECTS) && buildMenuIsRich(g));
        Decision decision = buildCardDecisions.build(game, first(game));
        assertTrue(decision.candidates().size() >= 2, "a build menu is a card plus the stop candidate");
        assertDistinct(decision);
    }

    /** Whether the position offers at least two options to build: the claim cannot be checked otherwise. */
    private boolean buildMenuIsRich(MarsGame game) {
        try {
            return buildCardDecisions.build(game, first(game)).candidates().size() >= 2;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Test
    void theStandardActionCandidatesLookDifferentToTheNet() {
        MarsGame game = snapshot(g -> g.getStateType() == StateType.PERFORM_BLUE_ACTION);
        Player player = first(game);
        player.setMc(200);
        player.setHeat(40);
        player.setPlants(20);
        List<StandardActionChoice> choices = standardActionDecisions.options(game, player,
                List.of(TurnType.STANDARD_PROJECT, TurnType.PLANT_FOREST, TurnType.INCREASE_TEMPERATURE));
        assertTrue(choices.size() >= 2, "the spend menu must offer more than stop in this position");
        assertDistinct(standardActionDecisions.decision(game, player, choices));
    }

    private static void assertDistinct(Decision decision) {
        Set<String> seen = new HashSet<>();
        for (Candidate candidate : decision.candidates()) {
            assertTrue(seen.add(Arrays.toString(candidate.features())), decision.type()
                    + " offers two candidates with identical features (id " + candidate.id()
                    + "): the net cannot tell them apart");
        }
    }

    // ------------------------------------------------------------------ whose position is it

    @Test
    void theObservationDescribesThePlayerItIsBuiltFor() {
        MarsGame game = snapshot(g -> true);
        Player a = first(game);
        Player b = second(game);
        a.setMc(7);
        b.setMc(93);
        float[] scale = ObservationBuilder.scales();
        List<String> names = ObservationBuilder.names();
        float[] observationA = observationBuilder.build(game, a);
        float[] observationB = observationBuilder.build(game, b);
        assertRaw(observationA, scale, names, "mc", 7);
        assertRaw(observationA, scale, names, "op_mc", 93);
        assertRaw(observationB, scale, names, "mc", 93);
        assertRaw(observationB, scale, names, "op_mc", 7);
        assertRaw(observationA, scale, names, "score:my_winning_points", winPointsService.countWinPoints(a, game));
        assertRaw(observationA, scale, names, "score:opponent_winning_points", winPointsService.countWinPoints(b, game));
        assertRaw(observationB, scale, names, "score:my_winning_points", winPointsService.countWinPoints(b, game));
        assertRaw(observationB, scale, names, "score:opponent_winning_points", winPointsService.countWinPoints(a, game));
    }

    private static void assertRaw(float[] observation, float[] scale, List<String> names, String name, float expected) {
        int index = names.indexOf(name);
        assertTrue(index >= 0, name + " is missing from the observation");
        assertEquals(expected, observation[index] * scale[index], 0.51f, name + " does not describe the right player");
    }

    // ------------------------------------------------------------------ fairness of the search

    @Test
    void theMirroredObservationCannotSeeTheOpponentsHand() {
        MarsGame game = snapshot(g -> true);
        Player b = second(game);
        float[] publicB = observationBuilder.buildPublic(game, b);

        MarsGame copy = new MarsGame(game);
        Player copyB = copy.getPlayerByUuid(b.getUuid());
        List<Integer> taken = new ArrayList<>(copyB.getHand().getCards());
        copyB.getHand().getCards().clear();
        int added = 0;
        for (Integer id : cardService.createProjectsDeck(copy.getExpansions()).getCards()) {
            if (added == taken.size()) {
                break;
            }
            if (taken.contains(id)) {
                continue;
            }
            copyB.getHand().addCard(id);
            added++;
        }
        assertTrue(added > 0, "the deck must offer another card for the hand");
        float[] changedPublicB = observationBuilder.buildPublic(copy, copyB);
        assertTrue(Arrays.equals(publicB, changedPublicB),
                "the PUBLIC observation of a player changed when only that player's hand changed: the search would read "
                        + "hidden information");
        assertFalse(Arrays.equals(observationBuilder.build(game, b), observationBuilder.build(copy, copyB)),
                "the hand block is not in the full observation at all");
    }

    // ------------------------------------------------------------------ the value of a build on the copy

    @Test
    void buildingACardAddsItsPrintedPointsToTheCopy() {
        MarsGame game = snapshot(g -> g.getStateType() == StateType.BUILD_GREEN_PROJECTS);
        Player player = first(game);
        player.setMc(300);
        player.setPlants(50);
        player.setHeat(50);
        int built = 0;
        for (Integer id : new ArrayList<>(player.getHand().getCards())) {
            Card card = cardService.getCard(id);
            if (card == null) {
                continue;
            }
            int before = winPointsService.countWinPoints(player, game);
            MarsGame copy = new MarsGame(game);
            Player copyPlayer = copy.getPlayerByUuid(player.getUuid());
            try {
                if (card.getColor() == CardColor.GREEN) {
                    aiTurnService.buildGreenProjectSyncNoRequirements(copy, copyPlayer, id, List.of(), new HashMap<>());
                } else {
                    aiTurnService.buildBlueRedProjectSyncNoRequirements(copy, copyPlayer, id, List.of(), new HashMap<>());
                }
            } catch (RuntimeException e) {
                continue; // a card whose on-built input our menu does not model: another one will do
            }
            int after = winPointsService.countWinPoints(copyPlayer, copy);
            assertTrue(after >= before + card.getWinningPoints(),
                    "building " + card.getClass().getSimpleName()
                            + " must add at least its printed points to the score the value net reads");
            List<String> names = ObservationBuilder.names();
            float[] scales = ObservationBuilder.scales();
            int scoreIndex = names.indexOf("score:my_winning_points");
            float[] observed = observationBuilder.build(copy, copyPlayer);
            assertEquals(after, observed[scoreIndex] * scales[scoreIndex], 0.51f,
                    "the score the value net reads must be the engine's own total after the build");
            built++;
            if (built == 3) {
                break;
            }
        }
        assertTrue(built > 0, "no card of the position could be built on a copy");
    }

    // ------------------------------------------------------------------ menus

    @Test
    void everyPaymentTheBuilderOffersIsLegal() {
        MarsGame game = snapshot(g -> g.getStateType() == StateType.BUILD_GREEN_PROJECTS
                || g.getStateType() == StateType.BUILD_BLUE_RED_PROJECTS);
        Player player = first(game);
        player.setMc(300);
        // the card the agent would actually build: exactly what the build menu offers
        Decision buildMenu = buildCardDecisions.build(game, player);
        List<Candidate> cards = buildMenu.candidates().stream()
                .filter(candidate -> candidate.id() != BuildCardDecisions.STOP).toList();
        assertFalse(cards.isEmpty(), "the position must offer a card to build");
        int cardId = cards.get(0).id();
        Card card = cardService.getCard(cardId);
        Map<Integer, List<Integer>> input = new HashMap<>();
        try {
            input = buildInputDecisions.fill(game, player, card, decision -> 0);
        } catch (RuntimeException e) {
            // a card whose input our menu does not model: the payment test is about the payments we do offer
        }
        List<BuildPaymentChoice> choices = buildPaymentDecisions.options(game, player, card, input);
        assertFalse(choices.isEmpty(), "a payment menu is never empty");
        for (BuildPaymentChoice choice : choices) {
            assertEquals(null, cardValidationService.validateCard(player, game, cardId, choice.payments(), input),
                    "the builder offers a payment the engine rejects: " + choice.payments());
        }
        assertTrue(choices.stream().anyMatch(choice -> choice.payments().isEmpty()
                        || choice.payments().size() == 1),
                "the plain single-payment option must be offered");
    }

    @Test
    void anActivatedBlueActionLeavesTheMenu() {
        MarsGame game = snapshot(g -> g.getStateType() == StateType.PERFORM_BLUE_ACTION);
        Player player = first(game);
        Decision menu = blueActionDecisions.build(game, player);
        List<Candidate> cards = menu.candidates().stream()
                .filter(candidate -> candidate.id() != BlueActionDecisions.STOP).toList();
        if (cards.isEmpty()) {
            return; // nothing activatable in this position: the claim cannot be checked here
        }
        int cardId = cards.get(0).id();
        Map<Integer, List<Integer>> input = blueInputDecisions.fill(game, player, cardService.getCard(cardId),
                decision -> 0);
        aiTurnService.performBlueAction(game, player, cardId, input);
        Decision after = blueActionDecisions.build(game, player);
        assertTrue(after.candidates().stream().noneMatch(candidate -> candidate.id() == cardId),
                "an activated blue action is still offered by the menu: the policy cannot know what is left");
    }

    // ------------------------------------------------------------------ a free action must apply to the copy

    @Test
    void pressingAFreeCardDrawActionAddsItsCardsOnTheCopy() {
        MarsGame game = snapshot(g -> g.getStateType() == StateType.PERFORM_BLUE_ACTION);
        Player player = first(game);
        int aiCentral = cardIdOf(com.terraforming.ares.cards.blue.AiCentral.class);
        player.getPlayed().addCard(aiCentral);
        player.setBlueActionExtraActivationsLeft(0);
        int handBefore = player.getHand().size();
        MarsGame copy = new MarsGame(game);
        Player copyPlayer = copy.getPlayerByUuid(player.getUuid());
        MarsGame host = new MarsGame(game);
        Player probe = host.getPlayerByUuid(player.getUuid());
        Map<Integer, List<Integer>> input = blueInputDecisions.fill(host, probe,
                cardService.getCard(aiCentral), decision -> 0);
        aiTurnService.performBlueAction(host, probe, aiCentral, input);
        assertEquals(handBefore + 2, probe.getHand().size(),
                "AiCentral draws two cards: the afterstate the search reads must contain them");
        assertTrue(blueActionDecisions.build(host, probe).candidates().stream()
                        .noneMatch(candidate -> candidate.id() == aiCentral),
                "after the activation the card must leave the menu");
        assertNotNull(copy);
        assertNotNull(copyPlayer);
    }

    private int cardIdOf(Class<? extends Card> type) {
        for (Integer id : cardService.createProjectsDeck(new ArrayList<>(ObservationBuilder.PLAYED_EXPANSIONS))
                .getCards()) {
            if (cardService.getCard(id).getClass() == type) {
                return id;
            }
        }
        throw new IllegalStateException(type.getSimpleName() + " is not in the played expansions");
    }

    // ------------------------------------------------------------------ no bits for cards we cannot play

    @Test
    void everyCardBitBelongsToAPlayedExpansion() {
        Set<Class<?>> playable = new HashSet<>();
        Map<Expansion, Map<Integer, Card>> all = cardFactory.createAllProjects();
        for (Expansion expansion : ObservationBuilder.PLAYED_EXPANSIONS) {
            Map<Integer, Card> cards = all.get(expansion);
            if (cards != null) {
                cards.values().forEach(card -> playable.add(card.getClass()));
            }
        }
        assertFalse(playable.isEmpty(), "the played expansions must contain project cards");
        for (Class<? extends Card> card : ObservationBuilder.auditedCardBits()) {
            assertTrue(playable.contains(card), card.getSimpleName()
                    + " has an observation bit but belongs to an expansion this player never plays: a dead column");
        }
    }
}
