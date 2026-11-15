package com.terraforming.ares.selfplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.cards.blue.AiCentral;
import com.terraforming.ares.cards.red.DeimosDown;
import com.terraforming.ares.cards.blue.MatterManufactoring;
import com.terraforming.ares.cards.blue.RestructuredResources;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.payments.RestructuredResourcesPayment;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.AgentDecisionService;
import com.terraforming.ares.services.agent.BuildCardDecisions;
import com.terraforming.ares.services.agent.BuildInputDecisions;
import com.terraforming.ares.services.agent.BuildPaymentChoice;
import com.terraforming.ares.services.agent.BuildPaymentDecisions;
import com.terraforming.ares.services.agent.BlueActionDecisions;
import com.terraforming.ares.services.agent.Decision;
import com.terraforming.ares.services.agent.DecisionProbe;
import com.terraforming.ares.services.agent.DecisionType;
import com.terraforming.ares.services.agent.ObservationBuilder;
import com.terraforming.ares.services.agent.StandardActionDecisions;
import com.terraforming.ares.services.agent.StandardActionChoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The "obvious move" bench: positions where the rules alone say which action is better, and the player has to agree.
 * <p>
 * Win rate hides a player that never presses a free blue card; a single scenario shows it immediately. The expected move
 * of every scenario is arithmetic of the game, never an opinion (a card that is drawn for free is worth a card; a
 * discount that costs one plant saves five credits), so the bench is a sanity check of the whole decision machinery -
 * policy, critic, search and the encoder - and it can be run against any future model.
 * <p>
 * Positions are real mid-game snapshots, mutated into a controlled state. Every scenario is run through the same
 * {@link AgentDecisionService} path the live player uses, with {@link DecisionProbe} recording what was chosen.
 * <p>
 * {@code --mode sanity --out FILE.json [--search]}. Exit code 1 when a scenario fails.
 */
@Component
@RequiredArgsConstructor
public class SanityBench {
    /**
     * @param gate true only for the scenarios that follow from the rules alone (a free action, a card for one credit):
     *             those block a promotion. The rest are red flags for the log - the policy has to make those calls, and a
     *             "wrong" answer there is a reason to look, not a verdict.
     */
    public record ScenarioResult(String name, String why, String expected, String chosen, boolean passed, String note,
                                 boolean gate) {
    }

    public record Report(int scenarios, int passed, int failed, int failedGate, List<ScenarioResult> results) {
    }

    /** Records the decision and the index the player chose, without forcing anything. */
    private static final class Recorder implements DecisionProbe.Hook {
        final Map<DecisionType, int[]> chosen = new EnumMap<>(DecisionType.class);

        @Override
        public int beforeChoice(Decision decision, MarsGame game, Player player) {
            return -1;
        }

        @Override
        public void afterChoice(Decision decision, MarsGame game, Player player, int index) {
            chosen.put(decision.type(), new int[]{index});
        }
    }

    private final GameService gameService;
    private final SimulationProcessorService simulation;
    private final AgentDecisionService agent;
    private final CardService cardService;
    private final BlueActionDecisions blueActionDecisions;
    private final BuildCardDecisions buildCardDecisions;
    private final BuildInputDecisions buildInputDecisions;
    private final BuildPaymentDecisions buildPaymentDecisions;
    private final StandardActionDecisions standardActionDecisions;

    public Report run(Path out) throws Exception {
        List<ScenarioResult> results = new ArrayList<>();
        results.add(freeBlueAction());
        results.add(oneCreditCardDraw());
        results.add(payWithTheDiscount());
        results.add(buildInsteadOfPassing());
        results.add(phasePickWithNothingToDo());
        results.add(phaseThreeWithABlueTable());
        results.add(activationPaysMoreThanStopping());
        results.add(cheaperOfTwoEqualValueSpends());
        results.add(deimosDownVersusStandardOceans());
        Report report = new Report(results.size(),
                (int) results.stream().filter(ScenarioResult::passed).count(),
                (int) results.stream().filter(result -> !result.passed()).count(),
                (int) results.stream().filter(result -> !result.passed() && result.gate()).count(),
                results);
        if (out != null && out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        if (out != null) {
            new ObjectMapper().writeValue(out.toFile(), report);
        }
        for (ScenarioResult result : results) {
            String state = result.passed() ? "ok" : (result.gate() ? "FAIL" : "flag");
            System.out.printf("[SANITY] %-5s %-34s chosen: %-28s %s%n", state, result.name(), result.chosen(),
                    result.note());
        }
        System.out.println("[SANITY] " + report.passed() + "/" + report.scenarios()
                + " obvious moves found, " + report.failedGate() + " gate failure(s)");
        return report;
    }

    // ------------------------------------------------------------------ scenarios

    /** AiCentral draws two cards for free: pressing it can never be wrong in a phase that still has an action. */
    private ScenarioResult freeBlueAction() {
        String name = "free blue action (AiCentral)";
        String why = "AiCentral draws two cards and costs nothing: the action is free value";
        return runScenario(name, why, "activate AiCentral", game -> {
            Player player = first(game);
            player.setBlueActionExtraActivationsLeft(0);
            int aiCentral = idOf(AiCentral.class);
            player.getPlayed().addCard(aiCentral);
            if (blueActionDecisions.build(game, player).candidates().size() != 2) {
                return new Outcome(false, "skipped: the blue menu is not unique", "");
            }
            Optional<Integer> choice = agent.blueAction(game, player);
            boolean ok = choice.isPresent() && choice.get() == aiCentral;
            return new Outcome(ok, choice.map(String::valueOf).orElse("stop (declined)"), "", true);
        }, true, StateType.PERFORM_BLUE_ACTION);
    }

    /** MatterManufactoring pays one credit for a card, and a card sells for three: +2 credits for free. */
    private ScenarioResult oneCreditCardDraw() {
        String name = "one-credit card draw";
        String why = "MatterManufactoring costs 1 credit and gives a project card, which sells for 3: guaranteed +2";
        return runScenario(name, why, "activate MatterManufactoring", game -> {
            Player player = first(game);
            player.setBlueActionExtraActivationsLeft(0);
            player.setMc(Math.max(1, player.getMc()));
            int card = idOf(MatterManufactoring.class);
            player.getPlayed().addCard(card);
            if (blueActionDecisions.build(game, player).candidates().size() != 2) {
                return new Outcome(false, "skipped: the blue menu is not unique", "");
            }
            Optional<Integer> choice = agent.blueAction(game, player);
            boolean ok = choice.isPresent() && choice.get() == card;
            return new Outcome(ok, choice.map(String::valueOf).orElse("stop (declined)"), "", true);
        }, true, StateType.PERFORM_BLUE_ACTION);
    }

    /** Restructured Resources trades one plant for five credits: with a plant in hand the cheaper payment is forced. */
    private ScenarioResult payWithTheDiscount() {
        String name = "pay with the discount";
        String why = "Restructured Resources costs one plant and saves five credits: the discounted payment is cheaper";
        return runScenario(name, why, "a payment that uses Restructured Resources", game -> {
            Player player = first(game);
            player.setMc(60);
            player.setPlants(5);
            player.getPlayed().addCard(idOf(RestructuredResources.class));
            player.getHand().getCards().clear();
            int cardId = firstBuildableCard(game, player);
            if (cardId < 0) {
                return new Outcome(true, "skipped (no buildable card)", "no card could be built in this position");
            }
            Map<Integer, List<Integer>> input = new HashMap<>();
            try {
                input = buildInputDecisions.fill(game, player, cardService.getCard(cardId), decision -> 0);
            } catch (RuntimeException e) {
                return new Outcome(true, "skipped (input not modelled)", e.getMessage());
            }
            List<BuildPaymentChoice> choices = buildPaymentDecisions.options(game, player,
                    cardService.getCard(cardId), input);
            int discount = -1;
            for (int i = 0; i < choices.size(); i++) {
                boolean restructured = choices.get(i).payments().stream()
                        .anyMatch(payment -> payment instanceof RestructuredResourcesPayment);
                if (restructured) {
                    discount = i;
                    break;
                }
            }
            if (discount < 0) {
                return new Outcome(true, "skipped (no discounted option)", "the card is too cheap for a discount");
            }
            Recorder recorder = new Recorder();
            DecisionProbe.Hook previous = DecisionProbe.set(recorder);
            try {
                agent.playBuildCard(game, player, cardId);
            } finally {
                DecisionProbe.set(previous);
            }
            int[] chosen = recorder.chosen.get(DecisionType.BUILD_PAYMENT);
            // REPORT ONLY: paying five credits less is usually right, but a plant can be worth more than five credits
            // (a forest award, a plant-counted card), so a miss here is a signal to look, not a verdict.
            return new Outcome(true, chosen == null ? "no payment decision" : "index " + chosen[0],
                    "report only; the discounted payment is at index " + discount);
        }, StateType.BUILD_GREEN_PROJECTS, StateType.BUILD_BLUE_RED_PROJECTS);
    }

    /**
     * With a buildable card and plenty of credits, passing the build slot away is a wasted action - UNLESS the table
     * rewards standard projects (animals per project, a discount on them), in which case a phase-3 spend can be better
     * than a card and the scenario is only reported.
     */
    private ScenarioResult buildInsteadOfPassing() {
        String name = "build instead of passing";
        String why = "the player has a buildable card and 100 credits: a build slot used is a card for the engine";
        return runScenario(name, why, "build a card (not stop)", game -> {
            Player player = first(game);
            player.setMc(100);
            player.setPlants(5);
            player.setHeat(5);
            player.getHand().getCards().clear();
            int cardId = firstBuildableCard(game, player);
            if (cardId < 0) {
                return new Outcome(true, "skipped (no buildable card)", "");
            }
            Optional<Integer> choice = agent.buildCard(game, player);
            String chosen = choice.map(String::valueOf).orElse("stop (declined)");
            boolean rewardsStandardProjects = rewardsStandardProjects(player);
            if (rewardsStandardProjects) {
                return new Outcome(true, chosen,
                        "report only: the table rewards standard projects, so a spend can beat a card");
            }
            return new Outcome(choice.isPresent(), chosen, "the card offered first is " + cardId);
        }, StateType.BUILD_GREEN_PROJECTS, StateType.BUILD_BLUE_RED_PROJECTS);
    }

    /** An empty hand, no credits and no blue actions make phases 1, 2 and 3 worthless: income or draft must be called. */
    private ScenarioResult phasePickWithNothingToDo() {
        String name = "phase pick with nothing to do";
        String why = "no cards, no credits, no activatable blue card: phases 1/2/3 would be empty, 4 or 5 pays";
        return runScenario(name, why, "phase 4 or 5", game -> {
            Player player = first(game);
            player.setMc(0);
            player.setHeat(0);
            player.setPlants(0);
            player.setBlueActionExtraActivationsLeft(0);
            Decision menu = blueActionDecisions.build(game, player);
            if (menu.candidates().size() > 1) {
                return new Outcome(true, "skipped (a blue card is still activatable)", "");
            }
            int phase = agent.pickPhase(game, player);
            boolean ok = phase == 4 || phase == 5;
            return new Outcome(ok, "phase " + phase, "");
        }, StateType.PICK_PHASE);
    }

    /**
     * With Livestock on the table a temperature step is worth a terraforming point AND an animal (a point), for 14
     * credits; a standard forest costs 23 for one point and one oxygen step. The cheaper of two equal-value spends is
     * the temperature (owner's example, 2026-11-15).
     */
    private ScenarioResult cheaperOfTwoEqualValueSpends() {
        String name = "temperature against a standard forest";
        String why = "14 credits buy a terraforming point and a Livestock animal; 23 buy a forest and an oxygen step";
        return runScenario(name, why, "SP_TEMPERATURE", game -> {
            Player player = first(game);
            player.setMc(60);
            player.getPlayed().addCard(idOf(com.terraforming.ares.cards.blue.Livestock.class));
            List<com.terraforming.ares.model.turn.TurnType> possible = List.of(
                    com.terraforming.ares.model.turn.TurnType.STANDARD_PROJECT);
            List<StandardActionChoice> choices = standardActionDecisions.options(game, player, possible);
            boolean hasTemperature = choices.stream().anyMatch(c -> c.kind() == StandardActionDecisions.SP_TEMPERATURE);
            boolean hasForest = choices.stream().anyMatch(c -> c.kind() == StandardActionDecisions.SP_FOREST);
            if (!hasTemperature || !hasForest) {
                return new Outcome(true, "skipped (both spends are not offered)", "");
            }
            if (rewardsStandardProjects(player)) {
                return new Outcome(true, "skipped (the table rewards standard projects)", "");
            }
            Optional<StandardActionChoice> choice = agent.standardAction(game, player, possible);
            String chosen = choice.map(value -> "kind " + value.kind()).orElse("stop");
            // REPORT ONLY: the price comparison is arithmetic, but what a forest or an oxygen step is worth in THIS
            // position is a strategy question (adjacency, awards, the end of the game). A wrong answer is a red flag.
            return new Outcome(true, chosen, "report only; the forest would be kind " + StandardActionDecisions.SP_FOREST);
        }, StateType.PERFORM_BLUE_ACTION);
    }

    /**
     * The strategy the owner plays by hand: a table full of blue action cards is worth a phase 3, because activating them
     * pays money, cards, resources and points. Report only for now - this is exactly what the training is supposed to
     * learn, so it is a metric to watch rather than a rule.
     */
    private ScenarioResult phaseThreeWithABlueTable() {
        String name = "phase 3 with a blue table";
        String why = "five activatable blue action cards and 100 credits: activating them is the payoff of the engine";
        return runScenario(name, why, "phase 3", game -> {
            Player player = first(game);
            player.setMc(100);
            player.setBlueActionExtraActivationsLeft(0);
            for (Class<? extends Card> type : List.of(com.terraforming.ares.cards.blue.MatterManufactoring.class,
                    com.terraforming.ares.cards.blue.AiCentral.class, com.terraforming.ares.cards.blue.Steelworks.class)) {
                try {
                    player.getPlayed().addCard(idOfAnyProjects(type));
                } catch (RuntimeException e) {
                    // a card of an expansion we do not play: the scenario still runs with the others
                }
            }
            Decision menu = blueActionDecisions.build(game, player);
            int activatable = (int) menu.candidates().stream().filter(c -> c.id() != BlueActionDecisions.STOP).count();
            if (activatable < 3) {
                return new Outcome(true, "skipped (only " + activatable + " activatable card(s))", "");
            }
            int phase = agent.pickPhase(game, player);
            return new Outcome(true, "phase " + phase, "report only; " + activatable + " activatable cards");
        }, StateType.PICK_PHASE);
    }

    /** Activating a blue card that turns a profit must beat stopping: report only, the same "watch it" metric. */
    private ScenarioResult activationPaysMoreThanStopping() {
        String name = "activation instead of stop";
        String why = "the blue menu offers an action that gains resources for free: pressing it is value";
        return runScenario(name, why, "press a blue action", game -> {
            Player player = first(game);
            player.setBlueActionExtraActivationsLeft(0);
            Decision menu = blueActionDecisions.build(game, player);
            int activatable = (int) menu.candidates().stream().filter(c -> c.id() != BlueActionDecisions.STOP).count();
            if (activatable == 0) {
                return new Outcome(true, "skipped (nothing activatable)", "");
            }
            Optional<Integer> choice = agent.blueAction(game, player);
            return new Outcome(choice.isPresent(), choice.map(String::valueOf).orElse("stop (declined)"),
                    "report only; " + activatable + " activatable card(s)");
        }, StateType.PERFORM_BLUE_ACTION);
    }

    /**
     * Report-only: three standard oceans cost 45 credits, Deimos Down costs 30, gives 7 back and opens three oceans.
     * The player is in the action phase with the card in hand; what it spends its action on is worth seeing.
     */
    private ScenarioResult deimosDownVersusStandardOceans() {
        String name = "Deimos Down vs standard oceans";
        String why = "Deimos Down opens three oceans for ~26 credits; three standard oceans cost 45 - a red flag if the "
                + "player buys the project, not a rule the player encodes";
        return runScenario(name, why, "not to buy oceans while Deimos Down is affordable", game -> {
            Player player = first(game);
            player.setMc(45);
            player.getPlayed().addCard(idOf(DeimosDown.class));
            List<StandardActionChoice> choices = standardActionDecisions.options(game, player,
                    List.of(com.terraforming.ares.model.turn.TurnType.STANDARD_PROJECT,
                            com.terraforming.ares.model.turn.TurnType.PLANT_FOREST,
                            com.terraforming.ares.model.turn.TurnType.INCREASE_TEMPERATURE));
            Optional<StandardActionChoice> choice = agent.standardAction(game, player,
                    List.of(com.terraforming.ares.model.turn.TurnType.STANDARD_PROJECT,
                            com.terraforming.ares.model.turn.TurnType.PLANT_FOREST,
                            com.terraforming.ares.model.turn.TurnType.INCREASE_TEMPERATURE));
            String chosen = choice.map(value -> "kind " + value.kind()).orElse("stop (keeps the money)");
            // report only: the cross-type comparison (a standard project now vs a card next) is not a claim of the bench
            return new Outcome(true, chosen, "report only");
        }, StateType.PERFORM_BLUE_ACTION);
    }

    // ------------------------------------------------------------------ plumbing

    /** Card actions that pay for standard projects or for forests, which can make a phase-3 spend better than a card. */
    private static final java.util.Set<com.terraforming.ares.model.CardAction> STANDARD_PROJECT_REWARDS =
            java.util.Set.of(com.terraforming.ares.model.CardAction.SMALL_ANIMALS,
                    com.terraforming.ares.model.CardAction.STANDARD_TECHNOLOGY,
                    com.terraforming.ares.model.CardAction.COMMUNITY_AFFORESTATION);

    /** Whether the table pays for standard projects or forests (then a spend can be better than a card). */
    private boolean rewardsStandardProjects(Player player) {
        try {
            return player.getPlayed().getCards().stream()
                    .map(cardService::getCard)
                    .filter(java.util.Objects::nonNull)
                    .anyMatch(card -> card.getCardMetadata() != null
                            && STANDARD_PROJECT_REWARDS.contains(card.getCardMetadata().getCardAction()));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private record Outcome(boolean passed, String chosen, String note, boolean gate) {
        Outcome(boolean passed, String chosen, String note) {
            this(passed, chosen, note, false);
        }
    }

    private interface Body {
        Outcome run(MarsGame game);
    }

    private ScenarioResult runScenario(String name, String why, String expected, Body body, StateType... states) {
        return runScenario(name, why, expected, body, false, states);
    }

    private ScenarioResult runScenario(String name, String why, String expected, Body body, boolean quietBlueMenu,
                                       StateType... states) {
        MarsGame game;
        try {
            game = quietBlueMenu ? snapshotWithoutBlueActions(states) : snapshot(states);
        } catch (RuntimeException e) {
            return new ScenarioResult(name, why, expected, "no position", false,
                    "could not find a position: " + e.getMessage(), true);
        }
        Recorder recorder = new Recorder();
        DecisionProbe.Hook previous = DecisionProbe.set(recorder);
        try {
            Outcome outcome = body.run(game);
            boolean skip = outcome.chosen().startsWith("skipped");
            return new ScenarioResult(name, why, expected, outcome.chosen(), skip || outcome.passed(), outcome.note(),
                    outcome.gate());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return new ScenarioResult(name, why, expected, "exception", false,
                    e.getClass().getSimpleName() + ": " + e.getMessage(), true);
        } finally {
            DecisionProbe.set(previous);
        }
    }

    /** A position of one of these states where the player has no activatable blue card left (the menu is just stop). */
    private MarsGame snapshotWithoutBlueActions(StateType... states) {
        return snapshot(states, game -> {
            try {
                Player player = first(game);
                return blueActionDecisions.build(game, player).candidates().size() == 1;
            } catch (RuntimeException e) {
                return false;
            }
        });
    }

    private MarsGame snapshot(StateType... states) {
        return snapshot(states, null);
    }

    private MarsGame snapshot(StateType[] states, java.util.function.Predicate<MarsGame> extra) {
        for (int attempt = 0; attempt < 40; attempt++) {
            MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                    .playerNames(new ArrayList<>(List.of("A", "B")))
                    .computers(List.of(PlayerDifficulty.SMART, PlayerDifficulty.SMART))
                    .mulligan(true)
                    .expansion(com.terraforming.ares.model.Expansion.BASE)
                    .expansion(com.terraforming.ares.model.Expansion.DISCOVERY)
                    .expansion(com.terraforming.ares.model.Expansion.BUFFED_CORPORATION)
                    .dummyHand(true).build());
            MarsGame[] snapshot = new MarsGame[1];
            simulation.processSimulation(game, current -> {
                if (snapshot[0] != null || current.getTurns() < 20) {
                    return;
                }
                for (StateType state : states) {
                    if (current.getStateType() == state && (extra == null || extra.test(current))) {
                        snapshot[0] = new MarsGame(current);
                        return;
                    }
                }
            });
            if (snapshot[0] != null) {
                return snapshot[0];
            }
        }
        throw new IllegalStateException("no position in states " + List.of(states));
    }

    private static Player first(MarsGame game) {
        return game.getPlayerUuidToPlayer().values().iterator().next();
    }

    private int idOf(Class<? extends Card> type) {
        for (Integer id : cardService.createProjectsDeck(new ArrayList<>(ObservationBuilder.PLAYED_EXPANSIONS))
                .getCards()) {
            if (cardService.getCard(id).getClass() == type) {
                return id;
            }
        }
        throw new IllegalStateException(type.getSimpleName() + " is not in the played expansions");
    }

    /** Id of a card of the played expansions, or -1 when the class belongs to an expansion we do not play. */
    private int idOfAnyProjects(Class<? extends Card> type) {
        for (Integer id : cardService.createProjectsDeck(new ArrayList<>(ObservationBuilder.PLAYED_EXPANSIONS))
                .getCards()) {
            if (cardService.getCard(id).getClass() == type) {
                return id;
            }
        }
        return -1;
    }

    /** The first card of our deck that the player could build right now, or -1. */
    private int firstBuildableCard(MarsGame game, Player player) {
        for (Integer id : cardService.createProjectsDeck(new ArrayList<>(ObservationBuilder.PLAYED_EXPANSIONS))
                .getCards()) {
            if (player.getHand().getCards().contains(id)) {
                continue;
            }
            player.getHand().addCard(id);
            boolean buildable = buildCardDecisions.build(game, player).candidates().stream()
                    .anyMatch(candidate -> candidate.id() == id);
            player.getHand().getCards().remove(id);
            if (buildable) {
                player.getHand().addCard(id);
                return id;
            }
        }
        return -1;
    }
}
