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
        if (!onePlySearch.isBusy()) {
            if (!onePlySearch.isBusy()) {
            decisionLogs.record(game, player, afterstate, 0);
        }
        }
    }
    private static final int[] PHASE_PREFERENCE = parsePhases(System.getProperty("ares.agent.preferPhase", ""));
    /** Owner's A/B (22/11): no phase 3 when no active card on the table can be used, even with the phase upgrade. */
    private static final boolean NO_EMPTY_PHASE3 =
            Boolean.parseBoolean(System.getProperty("ares.agent.heuristic.noEmptyPhase3", "false"));
    /** Owner's A/B (22/11): pick phase 5 with this probability when it is legal. */
    private static final boolean APPLY_TO_AGENT = Boolean.parseBoolean(System.getProperty("ares.agent.heuristic.applyToAgent", "false"));
    private static final double PHASE5_RATE =
            Math.max(0.0, Math.min(1.0, Double.parseDouble(System.getProperty("ares.agent.heuristic.phase5Rate", "0"))));
    /**
     * Colour practice of the data generator (off by default): {@code -Dares.agent.preferColor=red|green|blue} takes the first offered card of that
     * colour at the build and draft menus, so a corpus exists in which the plan was actually played. Not a rule of the player: it is off unless
     * asked for, and the shipped bundle never sets it. See {@link ColourNudge}.
     */
    private static final String PREFER_COLOUR = System.getProperty("ares.agent.preferColor", "").trim();
    private static final double COLOUR_NUDGE_RATE =
            Math.max(0.0, Math.min(1.0, Double.parseDouble(System.getProperty("ares.agent.colorNudgeRate", "1"))));
    private static final java.util.Set<DecisionType> COLOUR_NUDGE_TYPES =
            java.util.EnumSet.of(DecisionType.BUILD_CARD, DecisionType.DRAFT_CARDS);
    /**
     * Menus where the action REMOVES a card: there the colour nudge keeps the preferred colour and gives up the others, so "practice red" means
     * "keep the red cards and discard/sell the rest". Separate set because the direction of the nudge is inverted.
     */
    private static final java.util.Set<DecisionType> COLOUR_KEEP_TYPES =
            java.util.EnumSet.of(DecisionType.MULLIGAN, DecisionType.DISCARD_CARDS,
                    DecisionType.SELL_CARDS, DecisionType.SELL_CARDS_LAST_ROUND);
    /**
     * Tag practice of the data generator (off by default): {@code -Dares.agent.preferTag=microbe} takes cards carrying that tag at the build and draft
     * menus and keeps them where a card has to be given up. Reason (owner, 20/11): microbes are too few to be a strategy, but they may complement a
     * good result, and a plan can only be learned from games in which it was played. See {@link TagNudge}.
     */
    private static final String PREFER_TAG = System.getProperty("ares.agent.preferTag", "").trim();
    /**
     * Set practice of the data generator (off by default): {@code -Dares.agent.preferSet=microbe} chases the cards that hold, produce, consume or hand
     * over a microbe (see {@link MicrobeCards}). The owner's correction (20/11): the MICROBE TAG is the wrong definition - the plan is the resource.
     */
    private static final String PREFER_SET = System.getProperty("ares.agent.preferSet", "").trim();
    /**
     * How often the phase preference is allowed to fire (1 = always). Below 1 it becomes a preference rather than a compulsion, which is what a
     * corpus of "this phase is played more" needs if the player is to keep its own judgement in the other rounds (owner, 20/11).
     */
    private static final double PREFER_PHASE_RATE = Math.max(0.0, Math.min(1.0,
            Double.parseDouble(System.getProperty("ares.agent.preferPhaseRate", "1"))));
    private static final double TAG_NUDGE_RATE = Math.max(0.0, Math.min(1.0,
            Double.parseDouble(System.getProperty("ares.agent.tagNudgeRate",
                    System.getProperty("ares.agent.colorNudgeRate", "1")))));
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
    /**
     * {@code -Dares.agent.generation.saveAbove=N}: a MEASUREMENT instrument for the data, not a rule of the player. While
     * generating, a build is declined while the player holds more than N MC, so the corpus contains the state the player never
     * produces on its own: a large stockpile and, later in the same game, its conversion into points.
     * <p>
     * Why (owner, 19/11, from a human game): the value prices money correctly (200 MC -> +2.9 logit) but the player's own states
     * never exceed ~60 MC (p95 13), so the conversion line is outside its experience and it never contests the opponent's. The
     * games stay competent - only the spending is deferred. Negative (default) disables the instrument.
     */
    private static final int SAVE_ABOVE = Integer.parseInt(System.getProperty("ares.agent.generation.saveAbove", "-1"));
    /**
     * {@code -Dares.agent.explore.guided=BUILD_CARD:0.3,PICK_PHASE:0.3}: at the probability given by {@link #EXPLORE}, instead
     * of a UNIFORM random candidate, sample from {@code softmax(searchScore / temperature)} - the search's own opinion, blurred.
     * <p>
     * Why it matters (owner, 19/11): uniform exploration tries a strategy at a random moment with random cards, so the corpus
     * contains bad play of that strategy, and a corpus inherits the strength of the player that played it - the student ends up
     * at 21-33%. AlphaZero explores the SEARCH (root noise over plausible moves), which produces competent but different games.
     * That is the only kind of data that can teach a plan the player does not choose itself.
     */
    private static final Map<DecisionType, Double> GUIDED =
            parseExplore(System.getProperty("ares.agent.explore.guided", ""));

    /**
     * Records a decision unless a hypothetical computation is in progress (`OnePlySearch.isBusy`): a rollout plays hundreds of
     * decisions the game never took, and recording them pollutes the corpus with phantom rows and grows the log queue
     * without bound (measured 19/11: 921k pending rows and 8 GB of observation vectors, the generator dying every ~250 games).
     */
    private void record(MarsGame game, Player player, Decision decision, int index) {
        if (!onePlySearch.isBusy()) {
            decisionLogs.record(game, player, decision, index);
        }
    }

    /**
     * Targeted heuristics of the owner's A/B (22/11), applied only to {@link com.terraforming.ares.model.PlayerDifficulty#AGENT_V2}
     * so one seat of a self-play match can have them and the other cannot (models2 is pointed at the same bundle, so the only
     * difference is the rule):
     * <ul>
     *   <li>{@code -Dares.agent.heuristic.noEmptyPhase3=true}: never pick phase 3 when no active card on the table can be
     *       used, even with the phase upgrade - the owner's reading is that an empty phase 3 bought for the upgrade cards
     *       is a losing habit;</li>
     *   <li>{@code -Dares.agent.heuristic.phase5Rate=P}: pick phase 5 with probability P when it is legal (the owner's
     *       "1-2% more often").</li>
     * </ul>
     */
    private int heuristicPhase(MarsGame game, Player player, Decision decision, int id) {
        int phase = id;
        if (NO_EMPTY_PHASE3 && phase == 3 && !hasActivatableCard(game, player)) {
            phase = candidatePhase(decision, 5);
            if (phase < 0) {
                phase = candidatePhase(decision, 1);
            }
            if (phase < 0) {
                phase = candidatePhase(decision, 2);
            }
            if (phase < 0) {
                phase = candidatePhase(decision, 4);
            }
            if (phase < 0) {
                phase = id;
            }
        }
        if (PHASE5_RATE > 0 && phase != 5 && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < PHASE5_RATE) {
            int five = candidatePhase(decision, 5);
            if (five >= 0) {
                phase = five;
            }
        }
        return phase;
    }

    private boolean hasActivatableCard(MarsGame game, Player player) {
        for (Integer playedId : player.getPlayed().getCards()) {
            Card card = cardService.getCard(playedId);
            if (card == null || !card.isActiveCard()) {
                continue;
            }
            if (blueInputDecisions.canActivate(game, player, card)) {
                return true;
            }
        }
        return false;
    }

    private static int candidatePhase(Decision decision, int phase) {
        for (int i = 0; i < decision.candidates().size(); i++) {
            if (decision.candidates().get(i).id() == phase) {
                return i;
            }
        }
        return -1;
    }

    public int pickPhase(MarsGame game, Player player) {
        Integer committed = onePlySearch.planProbePhaseFor(player.getUuid());
        if (committed != null) {
            // A plan probe (see OnePlySearch.planProbe): the rollout is asked "if I commit to this phase", so it must not be
            // talked out of it by the policy. The engine rejects a repeat of the previous phase, so the commitment is only
            // used when it is legal.
            Decision probe = pickPhaseDecisions.build(game, player);
            for (int i = 0; i < probe.candidates().size(); i++) {
                if (probe.candidates().get(i).id() == committed) {
                    return committed;
                }
            }
        }
        Decision decision = pickPhaseDecisions.build(game, player);
        int forced = forcedPhase(decision);
        if (forced >= 0 && PREFER_PHASE_RATE < 1
                && java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= PREFER_PHASE_RATE) {
            // A phase preference is a preference, not a compulsion (owner, 20/11): at a rate below 1 the player keeps its own choice in the rounds
            // where the dice do not fire, so a corpus of "phase 5 play" is produced without clamping the player to it.
            forced = -1;
        }
        int index = forced >= 0 ? forced : -1;
        if (index < 0 && PlanCodes.rollNudge() && PlanCodes.preferBuildPhases(player)) {
            // Coverage nudge of the generator (bit 8), not a rule of the player: without it no recorded game of the policy
            // ever picks phases 1/2, so the outcome can never say whether a build-phase style pays. Conditional on holding
            // a card the phase could build - an unconditional nudge would only generate games that starve (the income phase
            // would then depend on the dummy), and the outcome would measure the starvation, not the phase.
            index = buildPhaseWithWork(decision, player);
        }
        if (index < 0) {
            int chosen = choose(game, player, decision);
            int id = decision.candidates().get(chosen).id();
            if ((player.getDifficulty() == com.terraforming.ares.model.PlayerDifficulty.AGENT_V2 || (APPLY_TO_AGENT && player.getDifficulty() == com.terraforming.ares.model.PlayerDifficulty.AGENT))) {
                int nudged = heuristicPhase(game, player, decision, chosen);
                return decision.candidates().get(nudged).id();
            }
            return id;
        }
        record(game, player, decision, index);
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
    /** Index of the STOP candidate of a build menu while the save instrument is on and the player is rich, else -1. */
    private static int saveProbeIndex(Decision decision, Player player) {
        if (SAVE_ABOVE < 0 || decision.type() != DecisionType.BUILD_CARD || player.getMc() <= SAVE_ABOVE) {
            return -1;
        }
        for (int i = 0; i < decision.candidates().size(); i++) {
            if (decision.candidates().get(i).id() == BuildCardDecisions.STOP) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Colour practice of the data generator (off unless {@code -Dares.agent.preferColor=red|green|blue} is set).
     *
     * <p>Why (owner, 20/11): the NETWORK gap is measured on events - NETWORK plays 8.6 red cards a game against our 4.2 - and a plan can only be
     * learned from games in which it was played. This is the same kind of instrument as the plan nudges: it changes which games are produced, not
     * what the player is allowed to do, and it is off by default.
     */
    private static int colourNudge(Decision decision) {
        boolean build = COLOUR_NUDGE_TYPES.contains(decision.type());
        boolean keep = COLOUR_KEEP_TYPES.contains(decision.type());
        if (PREFER_COLOUR.isEmpty() || (!build && !keep)) {
            return -1;
        }
        if (COLOUR_NUDGE_RATE < 1
                && java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= COLOUR_NUDGE_RATE) {
            return -1;
        }
        CardColor wanted;
        try {
            wanted = CardColor.valueOf(PREFER_COLOUR.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return -1;
        }
        return keep
                ? ColourNudge.indexOfNotColour(decision.candidates(), wanted)
                : ColourNudge.indexOfColour(decision.candidates(), wanted);
    }

    /**
     * Tag practice of the data generator (off unless {@code -Dares.agent.preferTag=microbe} is set): build and draft a card carrying the tag, and give
     * up untagged cards where a card must leave. Not a rule of the player - off by default, and the shipped bundle never sets it.
     */
    private int tagNudge(Decision decision) {
        boolean build = COLOUR_NUDGE_TYPES.contains(decision.type());
        boolean keep = COLOUR_KEEP_TYPES.contains(decision.type());
        if (PREFER_TAG.isEmpty() || (!build && !keep)) {
            return -1;
        }
        if (TAG_NUDGE_RATE < 1
                && java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= TAG_NUDGE_RATE) {
            return -1;
        }
        com.terraforming.ares.model.Tag wanted;
        try {
            wanted = com.terraforming.ares.model.Tag.valueOf(PREFER_TAG.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return -1;
        }
        return TagNudge.index(decision.candidates(),
                id -> {
                    Card card = cardService.getCard(id);
                    return card != null && card.getTags().contains(wanted);
                },
                keep);
    }

    /**
     * Set practice of the data generator (off unless {@code -Dares.agent.preferSet=microbe} is set): build and draft a card of the microbe economy, and
     * give up the others where a card must leave. A supplement, not a strategy - off by default.
     */
    private int setNudge(Decision decision) {
        boolean build = COLOUR_NUDGE_TYPES.contains(decision.type());
        boolean keep = COLOUR_KEEP_TYPES.contains(decision.type());
        if (!"microbe".equalsIgnoreCase(PREFER_SET) || (!build && !keep)) {
            return -1;
        }
        if (TAG_NUDGE_RATE < 1
                && java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= TAG_NUDGE_RATE) {
            return -1;
        }
        return TagNudge.index(decision.candidates(),
                id -> MicrobeCards.isMicrobe(cardService.getCard(id)),
                keep);
    }

    /**
     * The same instrument for the draft-bonus card family ({@code -Dares.agent.preferSet=draft}): build ExtendedResources, Interns,
     * UnitedPlanetaryAlliance and BacterialAggregates, keep them where a card must leave. See {@link DraftBonusCards}.
     */
    private int draftNudge(Decision decision) {
        boolean build = COLOUR_NUDGE_TYPES.contains(decision.type());
        boolean keep = COLOUR_KEEP_TYPES.contains(decision.type());
        if (!"draft".equalsIgnoreCase(PREFER_SET) || (!build && !keep)) {
            return -1;
        }
        if (TAG_NUDGE_RATE < 1
                && java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= TAG_NUDGE_RATE) {
            return -1;
        }
        return TagNudge.index(decision.candidates(),
                id -> DraftBonusCards.isDraftBonus(cardService.getCard(id)),
                keep);
    }

    private static int explore(Decision decision) {
        Double probability = EXPLORE.get(decision.type());
        if (probability == null || java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= probability) {
            return -1;
        }
        Double temperature = GUIDED.get(decision.type());
        float[] scores = temperature == null ? null : SearchScores.peek(decision);
        if (scores != null) {
            int guided = sampleByScores(scores, temperature);
            if (guided >= 0) {
                return guided;
            }
        }
        return java.util.concurrent.ThreadLocalRandom.current().nextInt(decision.candidates().size());
    }

    /**
     * One draw from {@code softmax(score / temperature)} over the candidates the search could score. Returns -1 when no
     * candidate has a score, so the caller falls back to a uniform draw.
     */
    private static int sampleByScores(float[] scores, double temperature) {
        if (scores == null || scores.length == 0) {
            return -1;
        }
        double best = Double.NEGATIVE_INFINITY;
        for (float score : scores) {
            if (!Float.isNaN(score) && !Float.isInfinite(score) && score > best) {
                best = score;
            }
        }
        if (best == Double.NEGATIVE_INFINITY) {
            return -1;
        }
        double[] weights = new double[scores.length];
        double total = 0;
        double scale = Math.max(1e-6, temperature * 1000.0);   // the scores are milli-logits
        for (int i = 0; i < scores.length; i++) {
            float score = scores[i];
            if (Float.isNaN(score) || Float.isInfinite(score)) {
                weights[i] = 0;
                continue;
            }
            weights[i] = Math.exp((score - best) / scale);
            total += weights[i];
        }
        if (total <= 0) {
            return -1;
        }
        double draw = java.util.concurrent.ThreadLocalRandom.current().nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            draw -= weights[i];
            if (draw <= 0 && weights[i] > 0) {
                return i;
            }
        }
        return -1;
    }

    public CorporationChoice pickCorporation(MarsGame game, Player player) {
        List<CorporationChoice> choices = corporationDecisions.options(game, player);
        int searched = onePlySearch.chooseCorporation(game, player, choices);
        int index = choose(game, player, corporationDecisions.decision(game, player, choices), searched);
        return choices.get(index);
    }

    /**
     * Cards to replace. Empty means the player keeps the opening hand. Each discarded card and the final stop are
     * separate recorded decisions, so the learned value of "discard this" does not depend on an arbitrary order being
     * the only way to name a set.
     */
    /**
     * {@code -Dares.agent.mulligan.cards=N}: force exactly N discards, chosen by the same ranking as always, then stop.
     * <p>
     * A measurement instrument, not a rule of the player: the owner's question (18/11) is whether an aggressive mulligan -
     * "discarding more than half the opening hand can be right" - pays, and exploration cannot answer it because with a high
     * exploration rate the recorded *choice* is a random card. Forcing the count keeps the card choice with the critic and
     * varies only the number, so N=0/2/5 answers the question the owner asked. 0 (the default) keeps the learned behaviour.
     */
    private static final int MULLIGAN_CARDS = Integer.parseInt(System.getProperty("ares.agent.mulligan.cards", "0"));

    public List<Integer> mulligan(MarsGame game, Player player) {
        if (MULLIGAN_CARDS > 0) {
            List<Integer> forced = new ArrayList<>();
            for (int step = 0; step < MULLIGAN_CARDS; step++) {
                onePlySearch.mulliganContext(forced);
                Decision decision = mulliganDecisions.build(game, player, forced);
                int id = decision.candidates().get(choose(game, player, decision)).id();
                if (id == MulliganDecisions.STOP) {
                    break;
                }
                forced.add(id);
            }
            return forced;
        }
        List<Integer> discard = new ArrayList<>();
        while (true) {
            onePlySearch.mulliganContext(discard);
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
                    record(game, player, decision, i);
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
                record(game, player, decision, i);
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
        // What the card does when it is built - which phase is upgraded, which tag is taken, where the animal goes, what
        // MarsUniversity discards - is where a build is won or lost. Each slot with more than one legal option is ranked by
        // the search (OnePlySearch.rankBuildSlot) and the executed index goes through choose() so the corpus records it.
        Map<Integer, List<Integer>> input = onePlySearch.buildInputs(game, player, card,
                (decision, choices, partial, slotCard) -> {
                    int ranked = onePlySearch.rankBuildSlot(game, player, slotCard, choices, partial);
                    return choose(game, player, decision, ranked);
                });
        if (input == null) {
            input = buildInputDecisions.fill(game, player, card, decision -> choose(game, player, decision));
        }
        List<BuildPaymentChoice> payments = buildPaymentDecisions.options(game, player, card, input);
        // A discount plan changes what the same card costs, so it is worth a real counterfactual rather than the policy's
        // guess: the search applies each plan to a copy (OnePlySearch.choosePayment) and the row records that plan.
        int searched = onePlySearch.choosePayment(game, player, cardId, input, payments);
        int index = choose(game, player, buildPaymentDecisions.decision(game, player, card, payments), searched);
        // A discount plan changes what the same card costs, so it is worth a real counterfactual rather than the policy's
        // guess: the search applies each plan to a copy and keeps the best afterstate (see OnePlySearch.choosePayment).
        int ranked = onePlySearch.choosePayment(game, player, cardId, input, payments);
        if (ranked >= 0) {
            index = ranked;
        }
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
                    record(game, player, decision, i);
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
            record(game, player, decision, index);
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
        // The input of a blue action is where the engine is actually played: which card is spent, where the animal goes,
        // which resource is converted. Each slot is ranked by the search when it has more than one legal option, and the
        // executed index goes through choose() so the corpus records the action the game really took.
        Map<Integer, List<Integer>> input = onePlySearch.blueInputs(game, player, card,
                (decision, choices, partial, slotCard) -> {
                    int ranked = onePlySearch.rankBlueSlot(game, player, slotCard, choices, partial);
                    return choose(game, player, decision, ranked);
                });
        if (input == null) {
            input = blueInputDecisions.fill(game, player, card, decision -> choose(game, player, decision));
        }
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
        // A double production with the upgrade bought is a guaranteed gain: take it, never stop (see GuaranteedActions).
        Optional<Integer> guaranteed = guaranteedActions.doubleIncome(game, player);
        if (guaranteed.isPresent()) {
            for (int i = 0; i < decision.candidates().size(); i++) {
                if (decision.candidates().get(i).id() == guaranteed.get()) {
                    record(game, player, decision, i);
                    return guaranteed;
                }
            }
        }
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
     * (no stop Р Р†Р вЂљРІР‚Сњ the engine forbids confirming while a project is still affordable), then confirm. Returns true when a
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
        return choose(game, player, decision, -1);
    }

    /**
     * @param searched what the search already decided for this decision when it needed context the decision itself does
     *                 not carry (a payment plan needs the card), or -1. It is passed IN so that the row records the
     *                 action that was actually taken: recording the policy's index and then acting differently would
     *                 teach the policy a choice the game never made.
     */
    private int choose(MarsGame game, Player player, Decision decision, int searched) {
        // A tree line in progress (see services.agent.tree.TreeDriver): the SEARCH chooses at our nodes, not the policy. This is
        // what makes the tree span several of our decisions instead of one menu. Recording is already off (the driver runs
        // inside OnePlySearch's hypothetical section), so these choices cannot reach the corpus.
        com.terraforming.ares.services.agent.tree.TreeDriver tree =
                com.terraforming.ares.services.agent.tree.TreeDriver.current();
        if (tree != null) {
            int fromTree = tree.chooseIndex(decision, player);
            if (fromTree >= 0) {
                return fromTree;
            }
        }
        if (RECORD_AFTERSTATES) {
            recordAfterstate(game, player);
            lastDecision.put(key(game, player), decision);
        }
        // Measurement / search harnesses (services.agent.DecisionProbe) see the state before the action is applied.
        DecisionProbe.Hook probe = DecisionProbe.current();
        int index = probe == null ? -1 : probe.beforeChoice(decision, game, player);
        if (index < 0) {
            // Corpus instrument (see SAVE_ABOVE): decline the build and keep the money, so the generated games enter the
            // stockpile states (and their conversion later) that the player never reaches by itself.
            index = saveProbeIndex(decision, player);
        }
        if (index < 0) {
            // Measured exploration of the data generator (off unless -Dares.agent.explore is set): a uniformly random
            // candidate for this decision type, so the recorded games contain lines the greedy player never plays.
            index = probe == null ? explore(decision) : -1;
        }
        if (index < 0) {
            // Colour practice of the data generator (off unless -Dares.agent.preferColor is set): take the first offered card
            // of that colour, so a corpus exists in which the player actually plays that plan. See ColourNudge.
            index = colourNudge(decision);
        }
        if (index < 0) {
            // Tag practice of the data generator (off unless -Dares.agent.preferTag is set): see TagNudge.
            index = tagNudge(decision);
        }
        if (index < 0) {
            // Set practice of the data generator (off unless -Dares.agent.preferSet is set): see MicrobeCards.
            index = setNudge(decision);
        }
        if (index < 0) {
            // Draft-bonus practice (off unless -Dares.agent.preferSet=draft): see DraftBonusCards.
            index = draftNudge(decision);
        }
        if (index < 0) {
            // One-ply search on the state value net (off unless -Dares.agent.search=oneply): the candidate is applied to a
            // copy and V(afterstate) decides. Falls back to the policy for every type / state it does not cover.
            index = searched >= 0 ? searched : onePlySearch.choose(decision, game, player);
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
            record(game, player, decision, index);
        } else {
            probe.afterChoice(decision, game, player, index);
        }
        return index;
    }
}




