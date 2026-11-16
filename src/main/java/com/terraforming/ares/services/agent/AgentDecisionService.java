package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Entry point used by the AI turn processors of the AGENT difficulty.
 * A trained model is used for a decision type only when one is loaded for that type; every other decision is played by
 * {@link DecisionAgent} (uniform random, unless a test replaces it).
 */
@Service
@RequiredArgsConstructor
public class AgentDecisionService {
    private final PickPhaseDecisions pickPhaseDecisions;
    private final CorporationDecisions corporationDecisions;
    private final MulliganDecisions mulliganDecisions;
    private final HandCardDecisions handCardDecisions;
    private final StandardActionDecisions standardActionDecisions;
    private final BuildCardDecisions buildCardDecisions;
    private final BuildPaymentDecisions buildPaymentDecisions;
    private final BuildInputDecisions buildInputDecisions;
    private final BlueActionDecisions blueActionDecisions;
    private final BlueInputDecisions blueInputDecisions;
    private final UnmiRtDecisions unmiRtDecisions;
    private final CollectIncomeDecisions collectIncomeDecisions;
    private final DecisionAgent agent;
    private final NeuralPolicy neuralPolicy;
    private final OnePlySearch onePlySearch;
    private final DecisionLogs decisionLogs;
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final GuaranteedActions guaranteedActions;
    private final ObservationBuilder observationBuilder;
    /**
     * The last decision of each (game, player): its action has been applied by the time the player decides again, and
     * that state is what the search reads as the afterstate of the action. Recording it here keeps the value training
     * data in the distribution the search asks about.
     */
    private final Map<String, Decision> lastDecision = new java.util.concurrent.ConcurrentHashMap<>();

    private static String key(MarsGame game, Player player) {
        return System.identityHashCode(game) + "/" + player.getUuid();
    }

    /**
     * Afterstates are a diagnostic stream (the value of the state one action later); off by default because they add a
     * third of the data volume and, measured 2026-11-15, training the critic on them costs 3.7 points of win rate.
     * {@code -Dares.agent.afterstates=true} turns them back on.
     */
    private static final boolean RECORD_AFTERSTATES =
            Boolean.parseBoolean(System.getProperty("ares.agent.afterstates", "false"));

    /** Records "the state one action after my previous decision" before the new decision is made. */
    private void recordAfterstate(MarsGame game, Player player) {
        if (!RECORD_AFTERSTATES) {
            return;
        }
        Decision previous = lastDecision.remove(key(game, player));
        if (previous == null || !decisionLogs.isRecording()) {
            return;
        }
        Decision afterstate = new Decision(DecisionType.AFTERSTATE, player.getUuid(),
                observationBuilder.build(game, player),
                List.of(new Candidate(0, new float[]{0f})));
        decisionLogs.record(game, player, afterstate, 0);
    }
    private static final int[] PHASE_PREFERENCE =
            parsePhases(System.getProperty("ares.agent.preferPhase", ""));
    /**
     * Exploration of the DATA GENERATOR, not of the player: {@code -Dares.agent.explore=BUILD_CARD:0.3,PICK_PHASE:0.5}
     * replaces the decision by a uniformly random candidate for the given decision type with the given probability.
     * <p>
     * Why this exists: training on games of one greedy player has a fixed point - the agent never calls phases 1/2/3 and
     * never builds an engine, so no game it plays (and therefore no value function trained on those games) can ever say
     * that an engine is worth building. Uniform random noise on individual decision heads is the cheapest way to obtain
     * the missing counterfactual games with their results, without copying anybody's strategy. Those games are used for
     * the critic (what does a position lead to) and never as a model of correct play.
     */
    /**
     * DATA GENERATOR ONLY ({@code -Dares.agent.pressBlueActions=true}): whenever a blue action can be pressed, press it.
     * A player that never does it produces no game in which a blue engine exists, so neither the critic nor the policy can
     * ever learn what an engine is worth (measured 2026-11-15: the value net prices "many blue actions" at nothing because
     * it has never seen a game with them). This is a data collection profile, not part of the player.
     */
    private static final boolean PRESS_BLUE_ACTIONS =
            Boolean.parseBoolean(System.getProperty("ares.agent.pressBlueActions", "false"));

    /**
     * DATA GENERATOR ONLY ({@code -Dares.agent.preferIncome=true}): among the cards the build menu offers, prefer one that
     * pays income. Measured causally (paired rollouts on 500 positions, 2026-11-15): forcing this plan for eight turns is
     * worth +1.7 +/- 1.4 points, while "press every blue action" is worth -1.2 +/- 1.2. The plan that pays for this player
     * is the economy, not the blue actions - so the games it generates are a little better than the plain ones, and the
     * policy can learn it from their outcomes.
     */
    private static final boolean PREFER_INCOME =
            Boolean.parseBoolean(System.getProperty("ares.agent.preferIncome", "false"));

    private static final Map<DecisionType, Double> EXPLORE =
            parseExplore(System.getProperty("ares.agent.explore", ""));

    public int pickPhase(MarsGame game, Player player) {
        Decision decision = pickPhaseDecisions.build(game, player);
        int forced = forcedPhase(decision);
        int index = forced >= 0 ? forced : -1;
        if (index < 0 && PlanCodes.rollNudge() && PlanCodes.preferBuildPhases(player)) {
            // Coverage nudge of the generator (bit 8), not a rule of the player: without it no recorded game of the policy
            // ever picks phases 1/2, so the outcome can never say whether a build-phase style pays. Conditional on holding
            // a card the phase could build - an unconditional nudge would only generate games that starve (the income phase
            // would then depend on the dummy), and the outcome would measure the starvation, not the phase.
            index = buildPhaseWithWork(decision, player);
        }
        if (index < 0) {
            return decision.candidates().get(choose(game, player, decision)).id();
        }
        decisionLogs.record(game, player, decision, index);
        return decision.candidates().get(index).id();
    }

    /** The build phase whose cards this hand actually holds: 1 for a green card, otherwise 2 for a blue/red one. */
    private int buildPhaseWithWork(Decision decision, Player player) {
        boolean green = false;
        boolean other = false;
        for (Integer id : player.getHand().getCards()) {
            Card card = cardService.getCard(id);
            if (card == null) {
                continue;
            }
            if (card.getColor() == CardColor.GREEN) {
                green = true;
            } else if (card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED) {
                other = true;
            }
        }
        if (green) {
            return firstCandidate(decision, 1);
        }
        return other ? firstCandidate(decision, 2) : -1;
    }

    private static int firstCandidate(Decision decision, int... ids) {
        for (int wanted : ids) {
            for (int i = 0; i < decision.candidates().size(); i++) {
                if (decision.candidates().get(i).id() == wanted) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Diagnostic override: {@code -Dares.agent.preferPhase=1,2} makes the agent take the first of those phases that is
     * legal. The phase pick is a first-class decision (the picker gets an extra build, a discount, an extra income or an
     * extra drafted card, see docs/GAME_RULES.md) and the agent picks income or draft in ~97% of the rounds. Forcing a
     * preference measures whether that is skill or habit; without the flag nothing changes.
     */
    private static int forcedPhase(Decision decision) {
        if (PHASE_PREFERENCE.length == 0) {
            return -1;
        }
        for (int wanted : PHASE_PREFERENCE) {
            for (int i = 0; i < decision.candidates().size(); i++) {
                if (decision.candidates().get(i).id() == wanted) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int[] parsePhases(String spec) {
        if (spec == null || spec.isBlank()) {
            return new int[0];
        }
        List<Integer> phases = new ArrayList<>();
        for (String part : spec.split(",")) {
            if (!part.isBlank()) {
                phases.add(Integer.parseInt(part.trim()));
            }
        }
        return phases.stream().mapToInt(Integer::intValue).toArray();
    }

    /** {@code TYPE:probability,...} - see {@link #EXPLORE}. Unknown types and bad numbers are a hard error. */
    private static Map<DecisionType, Double> parseExplore(String spec) {
        if (spec == null || spec.isBlank()) {
            return Map.of();
        }
        Map<DecisionType, Double> result = new java.util.EnumMap<>(DecisionType.class);
        for (String part : spec.split(",")) {
            if (part.isBlank()) {
                continue;
            }
            String[] pieces = part.trim().split(":");
            if (pieces.length != 2) {
                throw new IllegalArgumentException("explore entry must look like TYPE:probability, got '" + part + "'");
            }
            DecisionType type = DecisionType.valueOf(pieces[0].trim());
            double probability = Double.parseDouble(pieces[1].trim());
            if (probability < 0 || probability > 1) {
                throw new IllegalArgumentException("explore probability out of [0,1]: '" + part + "'");
            }
            result.put(type, probability);
        }
        return result;
    }

    /**
     * A uniformly random candidate for the configured decision types, or -1 to play as usual. Measured exploration only:
     * it runs while games are generated for the data, and it is not part of the player.
     */
    private static int explore(Decision decision) {
        Double probability = EXPLORE.get(decision.type());
        if (probability == null || java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= probability) {
            return -1;
        }
        return java.util.concurrent.ThreadLocalRandom.current().nextInt(decision.candidates().size());
    }

    public CorporationChoice pickCorporation(MarsGame game, Player player) {
        List<CorporationChoice> choices = corporationDecisions.options(game, player);
        int index = choose(game, player, corporationDecisions.decision(game, player, choices));
        return choices.get(index);
    }

    /**
     * Cards to replace. Empty means the player keeps the opening hand. Each discarded card and the final stop are
     * separate recorded decisions, so the learned value of "discard this" does not depend on an arbitrary order being
     * the only way to name a set.
     */
    public List<Integer> mulligan(MarsGame game, Player player) {
        List<Integer> discard = new ArrayList<>();
        while (true) {
            Decision decision = mulliganDecisions.build(game, player, discard);
            int id = decision.candidates().get(choose(game, player, decision)).id();
            if (id == MulliganDecisions.STOP) {
                return discard;
            }
            discard.add(id);
        }
    }

    /**
     * Cards the engine's pending discard wants removed. A revealed set (draft, draw-and-keep) is recorded as
     * {@link DecisionType#DRAFT_CARDS}; a discard from the whole hand as {@link DecisionType#DISCARD_CARDS}.
     * An empty result is the engine asking for nothing, and it is not a decision.
     */
    public List<Integer> discard(MarsGame game, Player player) {
        DiscardCardsTurn turn = (DiscardCardsTurn) player.getNextTurn();
        List<Integer> pool = turn.isOnlyFromSelectedCards() ? turn.getCards() : player.getHand().getCards();
        DecisionType type = turn.isOnlyFromSelectedCards() ? DecisionType.DRAFT_CARDS : DecisionType.DISCARD_CARDS;
        List<Integer> chosen = new ArrayList<>();
        for (int i = 0; i < turn.getSize(); i++) {
            Decision decision = handCardDecisions.build(type, game, player, pool, chosen, false);
            chosen.add(decision.candidates().get(choose(game, player, decision)).id());
        }
        return chosen;
    }

    /**
     * Cards to sell for their sell price right now. Empty means the agent keeps the hand and takes the other action
     * (build, blue action, and so on). The sale itself does not spend that action.
     */
    public List<Integer> sell(MarsGame game, Player player) {
        return takeUntilStop(DecisionType.SELL_CARDS, game, player, player.getHand().getCards(), 0);
    }

    /**
     * End-of-round sale. The hand must end at {@link Constants#MAX_HAND_SIZE_LAST_ROUND} or below, and selling more
     * than that is legal.
     */
    public List<Integer> sellLastRound(MarsGame game, Player player) {
        int minimum = Math.max(0, player.getHand().getCards().size() - Constants.MAX_HAND_SIZE_LAST_ROUND);
        return takeUntilStop(DecisionType.SELL_CARDS_LAST_ROUND, game, player, player.getHand().getCards(), minimum);
    }

    /**
     * One spend from the phase-3 menu, or empty when the agent stops and leaves blues/builds/skip to the fallback.
     * Helion exchange is offered one heat at a time; the next call sees the new heat.
     */
    public Optional<StandardActionChoice> standardAction(MarsGame game, Player player, List<TurnType> possibleTurns) {
        // Every legal spend stays on the menu. Comparing a standard project with a card in the hand is a strategy
        // decision that belongs to the policy (owner, 2026-11-15: the bench may watch it, the player may not encode it).
        List<StandardActionChoice> choices = standardActionDecisions.options(game, player, possibleTurns);
        int index = choose(game, player, standardActionDecisions.decision(game, player, choices));
        StandardActionChoice choice = choices.get(index);
        if (choice.kind() == StandardActionDecisions.STOP) {
            return Optional.empty();
        }
        return Optional.of(choice);
    }

    /**
     * Which hand card to build, or empty to stop. On-built inputs and payment are separate decisions filled by
     * {@link #playBuildCard}.
     */
    /**
     * DATA GENERATOR ONLY ({@code -Dares.agent.buildAlways=true}): never decline a build the player can afford.
     * <p>
     * Why it exists (measured 2026-11-16 with {@code --mode audit}): the critic prices +1 income at ~6.5 MC while the rules
     * make it worth ~15 MC at the median round, because in the games it was trained on income does not win - this player
     * hoards the money instead of converting it. That is a feedback loop: the critic is right about its own games, so the
     * player keeps hoarding. A critic trained on games where the player *does* build can break it, and this flag is how
     * those games are generated. It changes the data generator only; the deployed player never sets it.
     */
    private static final boolean BUILD_ALWAYS =
            Boolean.parseBoolean(System.getProperty("ares.agent.buildAlways", "false"));

    public Optional<Integer> buildCard(MarsGame game, Player player) {
        Decision decision = buildCardDecisions.build(game, player);
        if (BUILD_ALWAYS) {
            for (int i = 0; i < decision.candidates().size(); i++) {
                if (decision.candidates().get(i).id() != BuildCardDecisions.STOP) {
                    decisionLogs.record(game, player, decision, i);
                    return Optional.of(decision.candidates().get(i).id());
                }
            }
        }
        boolean nudge = PlanCodes.rollNudge();
        boolean preferIncome = PREFER_INCOME || (nudge && PlanCodes.preferIncome(player));
        boolean preferNonGreen = nudge && PlanCodes.preferNonGreen(player);
        for (int i = 0; i < decision.candidates().size(); i++) {
            int candidateId = decision.candidates().get(i).id();
            if (candidateId == BuildCardDecisions.STOP) {
                continue;
            }
            Card card = cardService.getCard(candidateId);
            if (card == null) {
                continue;
            }
            boolean income = card.getCardMetadata() != null && !card.getCardMetadata().getIncomes().isEmpty();
            boolean nonGreen = card.getColor() != com.terraforming.ares.model.CardColor.GREEN;
            if ((preferIncome && income) || (preferNonGreen && nonGreen)) {
                decisionLogs.record(game, player, decision, i);
                return Optional.of(candidateId);
            }
        }
        int id = decision.candidates().get(choose(game, player, decision)).id();
        if (id == BuildCardDecisions.STOP) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    public void playBuildCard(MarsGame game, Player player, int cardId) {
        Card card = cardService.getCard(cardId);
        Map<Integer, List<Integer>> input = buildInputDecisions.fill(game, player, card,
                decision -> choose(game, player, decision));
        List<BuildPaymentChoice> payments = buildPaymentDecisions.options(game, player, card, input);
        int index = choose(game, player, buildPaymentDecisions.decision(game, player, card, payments));
        aiTurnService.buildProject(game, player, cardId, payments.get(index).payments(), input);
    }

    /**
     * Which blue action to activate, or empty to stop. Inputs are a separate {@link DecisionType#BLUE_INPUT} sequence.
     */
    public Optional<Integer> blueAction(MarsGame game, Player player) {
        Decision decision = blueActionDecisions.build(game, player);
        if (PRESS_BLUE_ACTIONS || (PlanCodes.rollNudge() && PlanCodes.pressBlueActions(player))) {
            for (int i = 0; i < decision.candidates().size(); i++) {
                if (decision.candidates().get(i).id() != BlueActionDecisions.STOP) {
                    decisionLogs.record(game, player, decision, i);
                    return Optional.of(decision.candidates().get(i).id());
                }
            }
        }
        // A free action that leaves the position weakly better is taken by arithmetic, not by the network (the owner
        // plays the same way in his own agent). The label is still recorded, so the policy can learn it for the cases
        // where the rule does not apply.
        Optional<Integer> free = guaranteedActions.freeBlueAction(game, player);
        if (free.isPresent() && decision.candidates().stream().anyMatch(candidate -> candidate.id() == free.get())) {
            int index = 0;
            for (int i = 0; i < decision.candidates().size(); i++) {
                if (decision.candidates().get(i).id() == free.get()) {
                    index = i;
                    break;
                }
            }
            decisionLogs.record(game, player, decision, index);
            return free;
        }
        int id = decision.candidates().get(choose(game, player, decision)).id();
        if (id == BlueActionDecisions.STOP) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    public void playBlueAction(MarsGame game, Player player, int cardId) {
        Card card = cardService.getCard(cardId);
        Map<Integer, List<Integer>> input = blueInputDecisions.fill(game, player, card,
                decision -> choose(game, player, decision));
        aiTurnService.performBlueAction(game, player, cardId, input);
    }

    /**
     * Whether to take the UNMI +1 TR for 6 MC now. Empty means stop (skip / leave for a later visit this phase).
     * Not called when the action is unavailable.
     */
    public Optional<Boolean> unmiRt(MarsGame game, Player player, List<TurnType> possibleTurns) {
        if (!unmiRtDecisions.available(player, possibleTurns)) {
            return Optional.empty();
        }
        Decision decision = unmiRtDecisions.build(game, player);
        int id = decision.candidates().get(choose(game, player, decision)).id();
        if (id == UnmiRtDecisions.STOP) {
            return Optional.empty();
        }
        return Optional.of(Boolean.TRUE);
    }

    public void playUnmiRt(MarsGame game, Player player) {
        aiTurnService.unmiRtCorporationTurn(game, player);
    }

    /**
     * Card to double-collect under the phase-4 upgrade, or empty to collect income once. When the upgrade does not
     * apply or no green card can pay again, returns empty without recording a decision.
     */
    public Optional<Integer> collectIncome(MarsGame game, Player player) {
        if (!collectIncomeDecisions.needsChoice(player)) {
            return Optional.empty();
        }
        Decision decision = collectIncomeDecisions.build(game, player);
        int id = decision.candidates().get(choose(game, player, decision)).id();
        if (id == CollectIncomeDecisions.STOP) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    public void playCollectIncome(Player player, Integer doubleCardId) {
        aiTurnService.collectIncomeTurn(player, doubleCardId);
    }

    /**
     * After terraforming is complete: sell the hand (forced), buy remaining affordable standard projects one at a time
     * (no stop — the engine forbids confirming while a project is still affordable), then confirm. Returns true when a
     * turn was made.
     */
    public boolean endgame(MarsGame game, Player player, List<TurnType> possibleTurns) {
        if (!game.gameEndCondition()) {
            return false;
        }
        if (!player.getHand().isEmpty()) {
            Card corporation = cardService.getCard(player.getSelectedCorporationCard());
            if (corporation.getCardMetadata() != null
                    && corporation.getCardMetadata().getCardAction() == CardAction.HELION_CORPORATION
                    && player.getHeat() > 0) {
                player.setMc(player.getMc() + player.getHeat());
                player.setHeat(0);
            }
            aiTurnService.sellCards(player, game, new ArrayList<>(player.getHand().getCards()));
            return true;
        }
        List<StandardActionChoice> spends = standardActionDecisions.options(game, player, possibleTurns).stream()
                .filter(c -> c.kind() != StandardActionDecisions.STOP)
                .filter(c -> c.kind() == StandardActionDecisions.SP_OCEAN
                        || c.kind() == StandardActionDecisions.SP_FOREST
                        || c.kind() == StandardActionDecisions.SP_TEMPERATURE
                        || c.kind() == StandardActionDecisions.SP_INFRASTRUCTURE_PLANTS
                        || c.kind() == StandardActionDecisions.SP_INFRASTRUCTURE_HEAT)
                .toList();
        if (!spends.isEmpty()) {
            int index = choose(game, player, standardActionDecisions.decision(game, player, spends));
            playStandardAction(game, player, spends.get(index));
            return true;
        }
        aiTurnService.confirmGameEnd(game, player);
        return true;
    }

    /** Executes a non-stop choice from {@link #standardAction}. */
    public void playStandardAction(MarsGame game, Player player, StandardActionChoice choice) {
        switch (choice.kind()) {
            case StandardActionDecisions.EXCHANGE_ONE_HEAT -> aiTurnService.exchangeHeat(game, player, 1);
            case StandardActionDecisions.PLANT_FOREST -> aiTurnService.plantForest(game, player);
            case StandardActionDecisions.INCREASE_TEMPERATURE -> aiTurnService.increaseTemperature(game, player);
            case StandardActionDecisions.INCREASE_INFRASTRUCTURE_PLANTS,
                 StandardActionDecisions.INCREASE_INFRASTRUCTURE_HEAT ->
                    aiTurnService.increaseInfrastructure(player, game, choice.input());
            case StandardActionDecisions.SP_OCEAN, StandardActionDecisions.SP_FOREST,
                 StandardActionDecisions.SP_TEMPERATURE, StandardActionDecisions.SP_INFRASTRUCTURE_PLANTS,
                 StandardActionDecisions.SP_INFRASTRUCTURE_HEAT ->
                    aiTurnService.standardProjectTurn(game, player, choice.projectType(), choice.input());
            default -> throw new IllegalStateException("Cannot play standard action kind " + choice.kind());
        }
    }

    private List<Integer> takeUntilStop(DecisionType type, MarsGame game, Player player, List<Integer> pool, int minimum) {
        List<Integer> chosen = new ArrayList<>();
        while (true) {
            Decision decision = handCardDecisions.build(type, game, player, pool, chosen, chosen.size() >= minimum);
            int id = decision.candidates().get(choose(game, player, decision)).id();
            if (id == CardFeatures.STOP_ID) {
                return chosen;
            }
            chosen.add(id);
        }
    }

    private int choose(MarsGame game, Player player, Decision decision) {
        if (RECORD_AFTERSTATES) {
            recordAfterstate(game, player);
            lastDecision.put(key(game, player), decision);
        }
        // Measurement / search harnesses (services.agent.DecisionProbe) see the state before the action is applied.
        DecisionProbe.Hook probe = DecisionProbe.current();
        int index = probe == null ? -1 : probe.beforeChoice(decision, game, player);
        if (index < 0) {
            // Measured exploration of the data generator (off unless -Dares.agent.explore is set): a uniformly random
            // candidate for this decision type, so the recorded games contain lines the greedy player never plays.
            index = probe == null ? explore(decision) : -1;
        }
        if (index < 0) {
            // One-ply search on the state value net (off unless -Dares.agent.search=oneply): the candidate is applied to a
            // copy and V(afterstate) decides. Falls back to the policy for every type / state it does not cover.
            index = onePlySearch.choose(decision, game, player);
        }
        if (index < 0) {
            index = neuralPolicy.choose(decision,
                    player.getDifficulty() != null && player.getDifficulty().isStrongAgent());
        }
        if (index < 0) {
            index = agent.choose(decision);
        }
        if (index < 0 || index >= decision.candidates().size()) {
            throw new IllegalStateException("Agent chose " + index + " out of " + decision.candidates().size()
                    + " candidates of " + decision.type());
        }
        if (probe == null) {
            decisionLogs.record(game, player, decision, index);
        } else {
            probe.afterChoice(decision, game, player, index);
        }
        return index;
    }
}
