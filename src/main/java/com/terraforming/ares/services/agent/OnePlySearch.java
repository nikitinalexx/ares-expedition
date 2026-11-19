package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StandardProjectType;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.PhaseChoiceTurn;
import com.terraforming.ares.processors.turn.PickCorporationProcessor;
import com.terraforming.ares.processors.turn.PickPhaseProcessor;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.StateTransitionService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One-ply search on the state value network {@link MlpValueModel}: <b>apply the candidate to a copy of the game and ask
 * V how good the resulting position is</b>, then take the best candidate.
 * <p>
 * Why this shape (owner, 2026-11-14): the value of a card is not a property of the card. AntiGravity built in round 2
 * pays for a dozen projects, built in round 17 it pays for two; "this card is worth +6.5 points" is a rough average and
 * a rough estimate is exactly what loses games. The value has to be read off the whole state - incomes, both tables,
 * tags, resources, the hand - in the moment. {@link ObservationBuilder} is that state (it is the same vector the policy
 * sees, audited to contain no hidden information), so {@code V(observation after the move)} is the situational value of
 * the move and comparing it over the candidates is a cheap, fair lookahead:
 * <ul>
 *   <li>the copy is a fair copy: the observation of the afterstate is built for the moving player only, so the opponent's
 *       hand and the deck order can not enter the score;</li>
 *   <li>one copy + one build + one forward pass per candidate, no rollout of the opponent's turn (a rollout would
 *       condition on the opponent's actual hidden cards - that is the line this project does not cross);</li>
 *   <li>the network still decides the inner decisions of the build (on-built inputs, payment), exactly as it would in a
 *       real game.</li>
 * </ul>
 * The search is a <b>diagnostic / data-generation</b> tool until its win rate justifies it at deploy time: enabled only by
 * {@code -Dares.agent.search=oneply -Dares.agent.valueModel=DIR}. With search off, nothing here runs.
 * <p>
 * Not a {@link DecisionProbe.Hook}: a hook can only force an index, while this component needs the game and the player to
 * fork a copy. {@code AgentDecisionService} asks it before the policy and falls back to the policy when it returns -1.
 */
@Slf4j
@Service
public class OnePlySearch {
    private final ObservationBuilder observationBuilder;
    private final NeuralPolicy neuralPolicy;
    private final BuildInputDecisions buildInputDecisions;
    private final BuildPaymentDecisions buildPaymentDecisions;
    private final BlueInputDecisions blueInputDecisions;
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final StateTransitionService stateTransitionService;
    /** The engine's own phase-pick processor: the afterstate of a pick is built by the same code that plays it. */
    private final PickPhaseProcessor pickPhaseProcessor;
    private final SimulationProcessorService simulationProcessorService;
    private final WinPointsService winPointsService;
    private final List<MlpValueModel> models;
    /**
     * A separate critic for the phase pick, {@code -Dares.agent.valueModel.phase=DIR}. Entering a phase is a different
     * question from building a card: the afterstate is the START of a phase, whose distribution is not the distribution of
     * ordinary decision states. One net for both questions averages two different answers.
     */
    private final List<MlpValueModel> phaseModels;
    /**
     * {@code -Dares.agent.valueModel.mulligan=DIR,...}: a critic for the mulligan only.
     * <p>
     * Why it is a separate set: the mulligan is the one menu whose judgement needs the HAND IDENTITIES (which card is useless
     * for both offered corporations), and the card critics are trained with that block masked so they generalise. A critic
     * trained without the mask can say "this card adds nothing", and it is only asked here.
     */
    private final List<MlpValueModel> mulliganModels;
    /** The nets of the decision currently being scored; empty = the ordinary {@link #models} (see {@link #nets()}). */
    private final ThreadLocal<List<MlpValueModel>> currentNets = new ThreadLocal<>();
    private final Set<DecisionType> types;
    private final Set<DecisionType> policyChoiceTypes;
    /** Decision types where the search also decides "act or stop" (full comparison), not only the order of the menu. */
    private final Set<DecisionType> fullTypes;
    private final boolean rankOnly;
    private final boolean shuffle;
    private final boolean symmetric;
    /** Never decline a build while a card is available: the ranking decides, not the policy's stop. */
    private final boolean mustBuild;
    /**
     * {@code -Dares.agent.search.stopMargin=N}: when the policy wants to stop, build anyway if the best card's value beats
     * the value of stopping by more than N (same scaled units as the scores, i.e. logit x 1000).
     * <p>
     * Why a threshold and not a rule: "always build" measured -4.7 points and "never build" is the policy's own habit
     * (12.9 cards per game against SMART's 27). A threshold turns the same comparison into a dial whose value is measured,
     * and the value of a build is judged by the critic, not by a hand-written opinion about cards. 0 = off.
     */
    private final float stopMargin;
    /**
     * Per-type override of {@link #stopMargin}: {@code -Dares.agent.search.stopMargins=MULLIGAN:300,SELL_CARDS:300}.
     * <p>
     * The global value cannot be used for every menu: a build's afterstate beats the stop by ~1300 units (measured), so a
     * threshold that makes sense for a mulligan would make the player build every offered card, which measured -4.7 points.
     * The per-type form is what lets the critic gate *only* the menus where the policy's stop is a known collapse.
     */
    private final java.util.Map<DecisionType, Float> stopMargins;
    /**
     * {@code -Dares.agent.search.junkGate=MULLIGAN}: keep acting while the best candidate's removal costs nothing.
     * <p>
     * The owner's description of the mulligan (18/11): "discard the junk - sometimes there is a lot of it, sometimes
     * little, so the number is not fixed". The critic cannot see the hand identities (masked), but it does see the hand
     * SUMMARY (printed points, income counts). Removing a junk card leaves that summary unchanged, so
     * {@code V(after) - V(stop) = 0}; removing a good card lowers it, so the gap is negative. The rule "act while the gap is
     * not negative" is therefore exactly "discard the junk", and the count falls out of the hand instead of being chosen.
     */
    private final Set<DecisionType> junkGateTypes;
    /**
     * {@code -Dares.agent.search.playout=TYPE,...}: score a candidate by PLAYING it out with the policy for one round and
     * reading V at the next pick, instead of reading V at the one-action afterstate.
     * <p>
     * Why: the mulligan cannot be judged by V(hand minus a card) - the critic's hand-identity block is masked, so all it can
     * compare is the hand summary, and it then discards the whole hand (measured 19/11: 95.9% of removals "free", 9.94 of 10
     * cards). A playout puts the consequence of the hand into the state - the card that actually got built, the income that
     * actually arrived - which is the same trick {@code phasePlayout} uses for the phase pick.
     */
    private final Set<DecisionType> playoutTypes;
    /**
     * {@code -Dares.agent.search.junkMargin=<milli-logit>}: how close to the stop baseline a removal must be to count as
     * "free". A value function needs a small band (10 = 0.01 of V); a playout is a sampled comparison and needs a wider one,
     * so the band is a property instead of a constant.
     */
    private final float junkMargin;
    /**
     * {@code -Dares.agent.search.phaseBias=1:0.3,5:-0.1}: a measurement handicap added to a PHASE candidate's score, in
     * logits. It is not a rule of the player - it is the instrument for "is the phase picked too rarely", the same way
     * {@code ares.agent.mulligan.cards} measures the mulligan count: push the phase's share up and read the win rate. The
     * owner's hypothesis (19/11) is that phase 1 - green cards, income - is under-played at 8.7% of picks.
     */
    private final java.util.Map<Integer, Float> phaseBias;
    /**
     * Tolerance of the junk gate, in milli-logits of V. Removing a card that contributes nothing to the hand summary
     * leaves V unchanged up to float noise (a few milli); a card worth half a printed point moves V by more. 10 milli
     * (0.01 of V, i.e. a fraction of a victory point) is the "it costs me nothing" band - measured: with this band the
     * gate discards the zero-value cards and stops at the ones that carry points or income.
     */
    private static final float JUNK_EPSILON = 10f;
    /** The gate reads {@link #junkMargin}; this is the default when the property is absent. */
    /**
     * {@code -Dares.agent.search.debugJunk=true}: accumulate the gate's gap ({@code V(best removal) - V(stop)}) for the junk
     * types and print its distribution in the search summary. Without this number the gate is a guess: a gap that never
     * reaches zero means "no card looks free to the critic", which is a different failure from a wrong threshold.
     */
    private final boolean debugJunk = Boolean.parseBoolean(System.getProperty("ares.agent.search.debugJunk", "false"));
    private final AtomicLong junkSamples = new AtomicLong();
    private final AtomicLong junkFree = new AtomicLong();
    private final AtomicLong junkMin = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong junkMax = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong junkSum = new AtomicLong();
    /** How many mulligan afterstates founded a corporation. Zero means the two branches are not being explored at all. */
    private final AtomicLong mulliganCorps = new AtomicLong();
    /** Two builds deep: score card A together with the best follow-up card B (synergies), not only A alone. */
    private final boolean pairs;
    /** Generations to play out before the leaf value is read (0 = score the immediate afterstate, the original search). */
    private final int horizonTurns;
    /** Determinized playouts per candidate at the horizon (1 = one fair sample). */
    private final int samples;
    /**
     * Phase pick scored by PLAYING THE ROUND OUT (until the next pick) instead of by the start of the phase
     * ({@code -Dares.agent.search.phasePlayout=true}). The start of a phase is not a comparable position: entering phase 1
     * or 2 hands the player a build slot at once, while income and the draft are only collected when the phase is played,
     * so a one-step comparison systematically prefers the phases that pay in the afterstate. A round playout lets every
     * candidate collect its own payoff before the leaf value is read.
     */
    private final boolean phasePlayout;
    /**
     * {@code -Dares.agent.search.projectBuilds=true}: after applying a candidate, ADVANCE the game to the player's next
     * decision before reading V, instead of reading the value of a mid-turn afterstate.
     * <p>
     * Why (owner, 2026-11-16): NETWORK did not evaluate "the state right after the card"; it projected the phase
     * (`projectPhasePick`), built the card and recursed, so the value was read on a state of the same family the network
     * was trained on. Our afterstate - money gone, card on the table, the turn not finished - is a state the critic has
     * rarely seen, and an out-of-distribution state tends to score low, which biases every comparison against acting.
     * The stop candidate is the current decision state, so with this flag both sides of the comparison are decision states.
     */
    private final boolean projectBuilds;
    /** {@code -Dares.agent.search.batch=false} falls back to scoring the menu one candidate at a time. */
    private static final boolean BATCH =
            Boolean.parseBoolean(System.getProperty("ares.agent.search.batch", "false"));
    /**
     * {@code -Dares.agent.search.horizonStop=true}: score the "stop" candidate over the same horizon as the candidates
     * that act. A one-step comparison is structurally biased - an expensive action lowers V immediately (money out,
     * nothing in yet) and pays ten rounds later. Measured: 0.45 cards per game and a 1.2% build acceptance. With both
     * sides playing {@code horizon} turns of the policy before V is read, the delayed payoff is inside the window.
     */
    private final boolean horizonStop;
    private final int debug;
    private final PickCorporationProcessor pickCorporationProcessor;
    /** The offered corporations with their legal on-built inputs: the two branches the mulligan is judged against. */
    private final CorporationDecisions corporationDecisions;
    private final HandCardDecisions handCardDecisions;
    private final com.terraforming.ares.processors.turn.CollectIncomeTurnProcessor collectIncomeTurnProcessor;
    private final ThreadLocal<float[]> lastAfterstate = new ThreadLocal<>();
    /**
     * Cards already queued by the mulligan loop of this decision. The engine does not touch the hand until the player says
     * "stop" and then replaces the whole set at once, so the afterstate of the k-th discard is
     * {@code hand - (queued + candidate) + (k+1) random cards}. Without the queued set every step looked as good as the
     * first and the loop discarded until the candidate list ran out (measured 6.58 cards at the recording cap of 8, 9.10 at
     * 12) - the accumulation was lost, not the replacement.
     */
    private final ThreadLocal<List<Integer>> mulliganQueued = ThreadLocal.withInitial(List::of);
    /**
     * The replacement cards of this mulligan decision, drawn ONCE: the first {@code |queued|} are the stop baseline's
     * replacements, all of them are the candidate's. Sampling per candidate instead would compare "hand - c + random" with
     * "hand + random'" - two different draws - and the difference would carry the draw's noise on top of the card's value.
     * With common random numbers every arm sees the SAME replacements, so the gap is the value of the card alone. This is
     * the "sample it" the owner asked about, done so that the sample cancels.
     */
    private final ThreadLocal<List<Integer>> mulliganDraws = ThreadLocal.withInitial(List::of);
    /** Re-entrancy guard: the build of a candidate asks the policy for its inner decisions, never the search. */
    private final ThreadLocal<Boolean> busy = ThreadLocal.withInitial(() -> Boolean.FALSE);
    /** Whether this decision belongs to the second model set (AGENT_V2): passed to every policy call of this decision. */
    private final ThreadLocal<Boolean> strongAnswer = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private final AtomicLong decisions = new AtomicLong();
    private final AtomicLong candidates = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong disagreements = new AtomicLong();
    private final AtomicLong stops = new AtomicLong();
    private final AtomicLong menuDecisions = new AtomicLong();
    private final AtomicLong policyStops = new AtomicLong();
    private final AtomicLong gainSum = new AtomicLong();
    private final AtomicLong nanos = new AtomicLong();

    public OnePlySearch(ObservationBuilder observationBuilder, NeuralPolicy neuralPolicy,
                        BuildInputDecisions buildInputDecisions, BuildPaymentDecisions buildPaymentDecisions,
                        BlueInputDecisions blueInputDecisions, HandCardDecisions handCardDecisions,
                        AiTurnService aiTurnService, CardService cardService,
                        StateTransitionService stateTransitionService, PickPhaseProcessor pickPhaseProcessor,
                        PickCorporationProcessor pickCorporationProcessor,
                        com.terraforming.ares.services.agent.CorporationDecisions corporationDecisions,
                        com.terraforming.ares.processors.turn.CollectIncomeTurnProcessor collectIncomeTurnProcessor,
                        @Lazy SimulationProcessorService simulationProcessorService, WinPointsService winPointsService) {
        this.observationBuilder = observationBuilder;
        this.neuralPolicy = neuralPolicy;
        this.buildInputDecisions = buildInputDecisions;
        this.buildPaymentDecisions = buildPaymentDecisions;
        this.blueInputDecisions = blueInputDecisions;
        this.handCardDecisions = handCardDecisions;
        this.aiTurnService = aiTurnService;
        this.cardService = cardService;
        this.stateTransitionService = stateTransitionService;
        this.pickPhaseProcessor = pickPhaseProcessor;
        this.pickCorporationProcessor = pickCorporationProcessor;
        this.corporationDecisions = corporationDecisions;
        this.collectIncomeTurnProcessor = collectIncomeTurnProcessor;
        this.simulationProcessorService = simulationProcessorService;
        this.winPointsService = winPointsService;
        String mode = System.getProperty("ares.agent.search", "off");
        // One directory, or several separated by a comma: an ensemble averages the readings of every net, which is
        // cheaper than making one net better and has no shared point of failure.
        String spec = System.getProperty("ares.agent.valueModel", "").trim();
        this.types = parseTypes(System.getProperty("ares.agent.search.types", "BUILD_CARD"));
        /**
         * Menus where the policy's candidate is kept and the critic only gates the act (with stopMargin).
         * <p>
         * <b>Measured and rejected for the hand menus (18/11): 56.87% against SMART, 3000 games, against the same bundle's
         * 63.93% when the critic ranks those cards.</b> The intuition was that the critic cannot see the hand (its
         * hand-identity block is masked) while the policy can, so the policy should pick. It is the other way round: the
         * critic's summary-based ranking of "which card costs me least" beats the policy's own choice by seven points. Keep
         * this property empty unless a menu is measured to need it, and never enable it for the hand menus.
         */
        this.policyChoiceTypes = parseTypes(System.getProperty("ares.agent.search.policyChoice", ""));
        this.fullTypes = parseTypes(System.getProperty("ares.agent.search.fullTypes", ""));
        int configuredDebug = Integer.parseInt(System.getProperty("ares.agent.search.debug", "0"));
        boolean configuredRank = Boolean.parseBoolean(System.getProperty("ares.agent.search.rank", "true"));
        boolean configuredShuffle = Boolean.parseBoolean(System.getProperty("ares.agent.search.shuffle", "false"));
        boolean configuredSymmetric = Boolean.parseBoolean(System.getProperty("ares.agent.search.symmetric", "false"));
        boolean configuredMustBuild = Boolean.parseBoolean(System.getProperty("ares.agent.search.mustBuild", "false"));
        float configuredStopMargin = Float.parseFloat(System.getProperty("ares.agent.search.stopMargin", "0"));
        boolean configuredPairs = Boolean.parseBoolean(System.getProperty("ares.agent.search.pairs", "false"));
        int configuredHorizon = Integer.parseInt(System.getProperty("ares.agent.search.horizon", "0"));
        int configuredSamples = Math.max(1, Integer.parseInt(System.getProperty("ares.agent.search.samples", "1")));
        boolean configuredPhasePlayout =
                Boolean.parseBoolean(System.getProperty("ares.agent.search.phasePlayout", "false"));
        boolean configuredProjectBuilds =
                Boolean.parseBoolean(System.getProperty("ares.agent.search.projectBuilds", "false"));
        boolean configuredHorizonStop =
                Boolean.parseBoolean(System.getProperty("ares.agent.search.horizonStop", "false"));
        float configuredJunkMargin =
                Float.parseFloat(System.getProperty("ares.agent.search.junkMargin", "0"));
        if (!mode.equals("oneply") || spec.isEmpty()) {
            this.models = List.of();
            this.phaseModels = List.of();
            this.mulliganModels = List.of();
            this.debug = 0;
            this.rankOnly = true;
            this.shuffle = false;
            this.symmetric = false;
            this.mustBuild = false;
            this.stopMargin = 0f;
            this.stopMargins = new java.util.EnumMap<>(DecisionType.class);
            this.junkGateTypes = EnumSet.noneOf(DecisionType.class);
            this.playoutTypes = EnumSet.noneOf(DecisionType.class);
            this.junkMargin = JUNK_EPSILON;
            this.phaseBias = java.util.Map.of();
            this.pairs = false;
            this.horizonTurns = 0;
            this.samples = 1;
            this.phasePlayout = false;
            this.projectBuilds = false;
            this.horizonStop = false;
            return;
        }
        this.debug = configuredDebug;
        this.rankOnly = configuredRank;
        this.shuffle = configuredShuffle;
        this.symmetric = configuredSymmetric;
        this.mustBuild = configuredMustBuild;
        this.stopMargin = Math.max(0f, configuredStopMargin);
        this.stopMargins = parseMargins(System.getProperty("ares.agent.search.stopMargins", ""));
        this.junkGateTypes = parseTypes(System.getProperty("ares.agent.search.junkGate", ""));
        this.playoutTypes = parseTypes(System.getProperty("ares.agent.search.playout", ""));
        this.junkMargin = configuredJunkMargin;
        this.phaseBias = parsePhaseBias(System.getProperty("ares.agent.search.phaseBias", ""));
        this.pairs = configuredPairs;
        this.horizonTurns = Math.max(0, configuredHorizon);
        this.samples = configuredSamples;
        this.phasePlayout = configuredPhasePlayout;
        this.projectBuilds = configuredProjectBuilds;
        this.horizonStop = configuredHorizonStop;
        this.models = loadNets(spec);
        String phaseSpec = System.getProperty("ares.agent.valueModel.phase", "").trim();
        this.phaseModels = phaseSpec.isEmpty() ? List.of() : loadNets(phaseSpec);
        String mulliganSpec = System.getProperty("ares.agent.valueModel.mulligan", "").trim();
        this.mulliganModels = mulliganSpec.isEmpty() ? List.of() : loadNets(mulliganSpec);
        if (models.isEmpty()) {
            throw new IllegalStateException("ares.agent.valueModel has no directory: " + spec);
        }
        log.info("one-ply search: value model {}, types {}, observation hash {}",
                spec, types, models.get(0).observationHash());
        System.out.println("[SEARCH] playout " + playoutTypes + ", junk margin " + junkMargin);
        System.out.println("[SEARCH] junk gate " + junkGateTypes + ", policy choice " + policyChoiceTypes
                + ", stop margins " + stopMargins + ", symmetric " + symmetric + ", rankOnly " + rankOnly);
        System.out.println("[SEARCH] one-ply on " + types + ", value model " + spec
                + (phaseModels.isEmpty() ? "" : " (phase pick: "
                   + System.getProperty("ares.agent.valueModel.phase", "").trim() + ")")
                + " (" + models.size() + " net(s), obs " + models.get(0).obsSize()
                + ", hash " + models.get(0).observationHash()
                + ", rank=" + rankOnly + ", symmetric=" + symmetric + ", mustBuild=" + mustBuild + ", pairs=" + pairs
                + ", horizon=" + horizonTurns + " gen, samples=" + samples
                + (phasePlayout ? ", phase pick by a round playout" : "") + ")");
        Runtime.getRuntime().addShutdownHook(new Thread(this::report, "one-ply-report"));
    }

    /** Mean logit of every net of the ensemble: the score of one observation on its own. */
    private List<MlpValueModel> nets() {
        List<MlpValueModel> nets = currentNets.get();
        return nets == null ? models : nets;
    }

    private float logit(float[] observation) {
        List<MlpValueModel> nets = nets();
        float sum = 0f;
        for (MlpValueModel model : nets) {
            sum += model.logit(observation);
        }
        return sum / nets.size();
    }

    /**
     * Mean over the ensemble of {@code (logit(me) - logit(opponent)) / 2}: the same quantity as the win probability of
     * the position with the part of the error that belongs to the position rather than to the player cancelled.
     */
    private float symmetricScore(float[] mine, float[] opponent) {
        List<MlpValueModel> nets = nets();
        float sum = 0f;
        for (MlpValueModel model : nets) {
            sum += (model.logit(mine) - model.logit(opponent)) * 0.5f;
        }
        return sum / nets.size();
    }

    /** One directory, or several separated by a comma (an ensemble): every net must belong to the same encoder. */
    private static List<MlpValueModel> loadNets(String spec) {
        List<MlpValueModel> loaded = new ArrayList<>();
        for (String item : spec.split(",")) {
            String dir = item.trim();
            if (dir.isEmpty()) {
                continue;
            }
            try {
                MlpValueModel model = MlpValueModel.load(Path.of(dir));
                if (model.obsSize() != ObservationBuilder.size()) {
                    throw new IllegalStateException("The value model " + dir + " expects " + model.obsSize()
                            + " observation values, the encoder produces " + ObservationBuilder.size());
                }
                for (MlpValueModel other : loaded) {
                    if (!other.observationHash().equals(model.observationHash())) {
                        throw new IllegalStateException("The value model " + dir + " was trained on a different "
                                + "observation layout (" + model.observationHash() + " vs " + other.observationHash() + ")");
                    }
                }
                loaded.add(model);
            } catch (IOException e) {
                throw new UncheckedIOException("Can not load the value model " + dir, e);
            }
        }
        return List.copyOf(loaded);
    }

    private static java.util.Map<DecisionType, Float> parseMargins(String spec) {
        java.util.Map<DecisionType, Float> result = new java.util.EnumMap<>(DecisionType.class);
        if (spec == null || spec.isBlank()) {
            return result;
        }
        for (String part : spec.split(",")) {
            if (part.isBlank()) {
                continue;
            }
            String[] pieces = part.split(":");
            if (pieces.length != 2) {
                throw new IllegalArgumentException("stopMargins entry must look like TYPE:N, got '" + part + "'");
            }
            result.put(DecisionType.valueOf(pieces[0].trim()), Float.parseFloat(pieces[1].trim()));
        }
        return result;
    }

    private static Set<DecisionType> parseTypes(String spec) {
        Set<DecisionType> result = EnumSet.noneOf(DecisionType.class);
        for (String item : spec.split(",")) {
            if (!item.isBlank()) {
                result.add(DecisionType.valueOf(item.trim()));
            }
        }
        return result;
    }

    public boolean enabled() {
        return !models.isEmpty();
    }

    /**
     * @return the index of the candidate with the best afterstate, or -1 when the search does not apply (different
     * decision type, re-entrant call, or every candidate failed)
     */
    /** Tells the search which cards the mulligan loop has already queued (see {@link #mulliganQueued}). */
    public void mulliganContext(List<Integer> queued) {
        mulliganQueued.set(queued == null ? List.of() : List.copyOf(queued));
    }

    public int choose(Decision decision, MarsGame game, Player player) {
        if (models.isEmpty() || !types.contains(decision.type()) || busy.get()) {
            return -1;
        }
        busy.set(Boolean.TRUE);
        strongAnswer.set(player.getDifficulty() != null && player.getDifficulty().isStrongAgent());
        List<MlpValueModel> netsForThis = models;
        if (decision.type() == DecisionType.PICK_PHASE && !phaseModels.isEmpty()) {
            netsForThis = phaseModels;
        } else if (decision.type() == DecisionType.MULLIGAN && !mulliganModels.isEmpty()) {
            netsForThis = mulliganModels;
        }
        currentNets.set(netsForThis);
        long started = System.nanoTime();
        try {
            return search(decision, game, player);
        } finally {
            strongAnswer.set(Boolean.FALSE);
            busy.set(Boolean.FALSE);
            mulliganQueued.set(List.of());
            mulliganDraws.set(List.of());
            currentNets.remove();
            nanos.addAndGet(System.nanoTime() - started);
        }
    }

    /**
     * Ranks the payment plans of a card that was already chosen, by {@code V(afterstate)}.
     * <p>
     * Why this is a separate entry point and not another entry in {@link #types}: the payment decision does not carry the
     * card, so a candidate id alone (restructured / anaerobic / how much heat) cannot be applied to a copy. Here the caller
     * is the live build and it holds the card and the input, so the afterstate of each plan is exactly one build with that
     * plan. The menu is tiny in practice (mean 1.4 plans, 10% of decisions have more than two), so every plan is scored -
     * no cap, no heuristic, and a build with a single legal plan costs nothing.
     *
     * @return the index of the plan with the best afterstate, or -1 when the search does not apply (payment menu not
     *         ranked, one plan only, re-entrant call, or every plan failed to apply)
     */
    public int choosePayment(MarsGame game, Player player, int cardId, Map<Integer, List<Integer>> input,
                             List<BuildPaymentChoice> plans) {
        if (models.isEmpty() || !types.contains(DecisionType.BUILD_PAYMENT) || busy.get() || plans.size() < 2) {
            return -1;
        }
        busy.set(Boolean.TRUE);
        strongAnswer.set(player.getDifficulty() != null && player.getDifficulty().isStrongAgent());
        currentNets.set(models);
        long started = System.nanoTime();
        try {
            Card card = cardService.getCard(cardId);
            int best = -1;
            float bestScore = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < plans.size(); i++) {
                try {
                    MarsGame copy = new MarsGame(game);
                    Player me = copy.getPlayerByUuid(player.getUuid());
                    if (card.getColor() == com.terraforming.ares.model.CardColor.GREEN) {
                        aiTurnService.buildGreenProjectSync(copy, me, cardId, plans.get(i).payments(), input);
                    } else {
                        aiTurnService.buildBlueRedProjectSync(copy, me, cardId, plans.get(i).payments(), input);
                    }
                    float score = logit(observationBuilder.build(copy, me));
                    if (score > bestScore) {
                        bestScore = score;
                        best = i;
                    }
                } catch (RuntimeException ignored) {
                    // a plan the engine rejects on a copy is simply not a candidate for the ranking
                }
            }
            return best;
        } finally {
            strongAnswer.set(Boolean.FALSE);
            busy.set(Boolean.FALSE);
            currentNets.remove();
            nanos.addAndGet(System.nanoTime() - started);
        }
    }

    /**
     * Plays out the discards a corporation puts in the queue, so the afterstate is the settled position.
     * <p>
     * Zetacell draws five cards and then forces a discard of four (`DiscardCardsTurn`, expected as the next turn). The
     * engine applies that turn asynchronously during the corporation phase, so on a copy it would never happen and V
     * would see five extra cards and none of the four it has to give back - an afterstate that flatters Zetacell. The
     * cards are chosen by the policy ({@link HandCardDecisions#build}), because which four to keep is exactly the part the
     * critic is not trusted with yet; the settlement only has to be the position Zetacell really leads to.
     */
    private void settleQueuedDiscards(MarsGame copy, Player me) {
        for (int guard = 0; guard < 8; guard++) {
            com.terraforming.ares.model.turn.Turn next = me.getNextTurn();
            if (!(next instanceof com.terraforming.ares.model.turn.DiscardCardsTurn discard)
                    || !discard.expectedAsNextTurn()) {
                return;
            }
            List<Integer> chosen = new ArrayList<>();
            for (int i = 0; i < discard.getSize(); i++) {
                Decision slot = handCardDecisions.build(DecisionType.DISCARD_CARDS, copy, me,
                        me.getHand().getCards(), chosen, false);
                int index = innerChoice(slot);
                if (index < 0 || index >= slot.candidates().size()) {
                    break;
                }
                int id = slot.candidates().get(index).id();
                if (id == CardFeatures.STOP_ID || !me.getHand().getCards().contains(id)) {
                    break;
                }
                chosen.add(id);
            }
            for (int id : chosen) {
                me.getHand().removeCard(id);
            }
            me.removeNextTurn();
        }
    }

    /**
     * The blue action's input slots, decided by {@code chooser} - which the caller wires to {@link #rankBlueSlot}.
     * Returns null when the input menus are not ranked, so the caller keeps the policy's own fill.
     */
    public Map<Integer, List<Integer>> blueInputs(MarsGame game, Player player, Card card,
                                                  BlueInputDecisions.SlotChooser chooser) {
        if (models.isEmpty() || !types.contains(DecisionType.BLUE_INPUT)) {
            return null;
        }
        return blueInputDecisions.fillRanked(game, player, card, chooser);
    }

    /**
     * The build's input slots, decided by {@code chooser} - which the caller wires to {@link #rankBuildSlot}. Returns null
     * when the input menus are not ranked, so the caller keeps the policy's own fill.
     */
    public Map<Integer, List<Integer>> buildInputs(MarsGame game, Player player, Card card,
                                                   BuildInputDecisions.SlotChooser chooser) {
        if (models.isEmpty() || !types.contains(DecisionType.BUILD_INPUT)) {
            return null;
        }
        return buildInputDecisions.fillRanked(game, player, card, chooser);
    }

    /**
     * Ranks one slot of a card's input by {@code V(afterstate)} of the whole build: the candidate is put into the partial
     * input, the remaining slots are filled by the policy, the card is built on a copy and the afterstate is read.
     * <p>
     * This covers the owner's list: the tag of a dynamic card decides how many times Decomposers / ViralEnhancers /
     * MarsUniversity trigger (they are asked after the tag), Decomposers' "take a card" needs enough microbes on the card,
     * MarsUniversity cannot discard the card being built, and Filter Feeders' animal follows a microbe placed on another
     * card. A candidate whose completion the engine rejects is retried with the first option of every remaining slot
     * before it is dropped, so an option is not silently lost to a bad policy completion.
     */
    public int rankBuildSlot(MarsGame game, Player player, Card card, List<BuildInputChoice> choices,
                             Map<Integer, List<Integer>> partial) {
        if (models.isEmpty() || !types.contains(DecisionType.BUILD_INPUT) || busy.get() || choices.size() < 2) {
            return -1;
        }
        busy.set(Boolean.TRUE);
        strongAnswer.set(player.getDifficulty() != null && player.getDifficulty().isStrongAgent());
        currentNets.set(models);
        long started = System.nanoTime();
        try {
            int best = -1;
            float bestScore = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < choices.size(); i++) {
                Float score = scoreBuildWith(game, player, card, partial, choices.get(i), true);
                if (score == null) {
                    score = scoreBuildWith(game, player, card, partial, choices.get(i), false);
                }
                if (score != null && score > bestScore) {
                    bestScore = score;
                    best = i;
                }
            }
            return best;
        } finally {
            strongAnswer.set(Boolean.FALSE);
            busy.set(Boolean.FALSE);
            currentNets.remove();
            nanos.addAndGet(System.nanoTime() - started);
        }
    }

    /**
     * @param policyCompletion true: the remaining slots and the payment plan are the policy's choices; false: the first
     *                         option of every remaining slot, which is the cheap fallback when the policy's completion is
     *                         not legal for this candidate.
     */
    private Float scoreBuildWith(MarsGame game, Player player, Card card, Map<Integer, List<Integer>> partial,
                                 BuildInputChoice choice, boolean policyCompletion) {
        try {
            MarsGame copy = new MarsGame(game);
            Player me = copy.getPlayerByUuid(player.getUuid());
            Map<Integer, List<Integer>> input = new HashMap<>(partial);
            buildInputDecisions.apply(input, choice, card);
            BuildInputDecisions.SlotChooser chooser = policyCompletion
                    ? (decision, slotChoices, slotInput, slotCard) -> innerChoice(decision)
                    : (decision, slotChoices, slotInput, slotCard) -> 0;
            Map<Integer, List<Integer>> full = buildInputDecisions.fillRanked(copy, me, card, input, chooser);
            List<BuildPaymentChoice> plans = buildPaymentDecisions.options(copy, me, card, full);
            List<com.terraforming.ares.model.payments.Payment> payments =
                    plans.get(Math.max(0, innerChoice(buildPaymentDecisions.decision(copy, me, card, plans)))).payments();
            if (card.getColor() == com.terraforming.ares.model.CardColor.GREEN) {
                aiTurnService.buildGreenProjectSync(copy, me, card.getId(), payments, full);
            } else {
                aiTurnService.buildBlueRedProjectSync(copy, me, card.getId(), payments, full);
            }
            return logit(observationBuilder.build(copy, me));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Ranks one slot of a blue action's input by {@code V(afterstate)}: the slots already filled are replayed, the
     * candidate is applied, the remaining slots are filled by the policy, and the action is performed on the copy.
     * <p>
     * This is what "in what order do I press the actions, what do I spend, where does the microbe go" needs: the parent
     * menu ({@link DecisionType#PERFORM_BLUE_ACTION}) says which action, this says how it is executed.
     */
    public int rankBlueSlot(MarsGame game, Player player, Card card, List<BlueInputChoice> choices,
                            Map<Integer, List<Integer>> partial) {
        if (models.isEmpty() || !types.contains(DecisionType.BLUE_INPUT) || busy.get() || choices.size() < 2) {
            return -1;
        }
        busy.set(Boolean.TRUE);
        strongAnswer.set(player.getDifficulty() != null && player.getDifficulty().isStrongAgent());
        currentNets.set(models);
        long started = System.nanoTime();
        try {
            int best = -1;
            float bestScore = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < choices.size(); i++) {
                try {
                    MarsGame copy = new MarsGame(game);
                    Player me = copy.getPlayerByUuid(player.getUuid());
                    Map<Integer, List<Integer>> input = new HashMap<>(partial);
                    blueInputDecisions.apply(input, choices.get(i), card, me);
                    Map<Integer, List<Integer>> full = blueInputDecisions.fillRanked(copy, me, card, input,
                            (decision, slotChoices, slotInput, slotCard) -> innerChoice(decision));
                    aiTurnService.performBlueAction(copy, me, card.getId(), full);
                    float score = logit(observationBuilder.build(copy, me));
                    if (score > bestScore) {
                        bestScore = score;
                        best = i;
                    }
                } catch (RuntimeException ignored) {
                    // an input the engine rejects on a copy is not a candidate for the ranking
                }
            }
            return best;
        } finally {
            strongAnswer.set(Boolean.FALSE);
            busy.set(Boolean.FALSE);
            currentNets.remove();
            nanos.addAndGet(System.nanoTime() - started);
        }
    }

    /**
     * Ranks the offered corporations (each with the input its on-built effect needs) by {@code V(afterstate)}.
     * <p>
     * The corporation is the opening commitment: it decides the starting bonus, and - as the owner put it - what the
     * mulligan keeps and what the first builds aim at (a corporation that marks an award is worth chasing that award).
     * The engine applies it synchronously in {@code PickCorporationProcessor}, so the afterstate on a copy is exact.
     * Offered once per game, so the cost is irrelevant.
     *
     * @return the index of the corporation with the best afterstate, or -1 when the search does not apply
     */
    public int chooseCorporation(MarsGame game, Player player, List<CorporationChoice> choices) {
        if (models.isEmpty() || !types.contains(DecisionType.PICK_CORPORATION) || busy.get() || choices.size() < 2) {
            return -1;
        }
        busy.set(Boolean.TRUE);
        strongAnswer.set(player.getDifficulty() != null && player.getDifficulty().isStrongAgent());
        currentNets.set(models);
        long started = System.nanoTime();
        try {
            int best = -1;
            float bestScore = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < choices.size(); i++) {
                try {
                    MarsGame copy = new MarsGame(game);
                    Player me = copy.getPlayerByUuid(player.getUuid());
                    CorporationChoice choice = choices.get(i);
                    pickCorporationProcessor.processTurn(new com.terraforming.ares.model.turn.CorporationChoiceTurn(
                            player.getUuid(), choice.corporationId(), choice.input()), copy);
                    settleQueuedDiscards(copy, me);
                    float score = logit(observationBuilder.build(copy, me));
                    if (score > bestScore) {
                        bestScore = score;
                        best = i;
                    }
                } catch (RuntimeException ignored) {
                    // a corporation the engine rejects on a copy is not a candidate for the ranking
                }
            }
            return best;
        } finally {
            strongAnswer.set(Boolean.FALSE);
            busy.set(Boolean.FALSE);
            currentNets.remove();
            nanos.addAndGet(System.nanoTime() - started);
        }
    }

    private int search(Decision decision, MarsGame game, Player player) {
        List<Candidate> options = decision.candidates();
        // STANDARD_ACTION codes its stop as kind 0, the card decisions use the shared STOP id.
        int stopId = decision.type() == DecisionType.STANDARD_ACTION
                ? StandardActionDecisions.STOP : BuildCardDecisions.STOP;
        boolean rankThis = rankOnly && !fullTypes.contains(decision.type());
        boolean debugThis = debug > 0 && decisions.get() < debug;
        // The stop candidate changes nothing: its afterstate is the current state, whose observation the decision
        // already carries. Everything else is built on a copy and scored by V(afterstate).
        float[] stopObs = decision.observation();
        if (decision.type() == DecisionType.MULLIGAN) {
            mulliganDraws.set(drawMulliganCards(game, mulliganQueued.get().size() + 1));
        }
        if (junkGateTypes.contains(decision.type()) && !mulliganQueued.get().isEmpty()) {
            // "Stop" is not the current hand: when the player stops, the engine still discards the queued cards and deals
            // as many replacements. Comparing a candidate that already replaced them against a baseline that still holds
            // them is comparing two different hands, so the baseline is the post-stop state as well.
            float[] shifted = mulliganStopObservation(game, player);
            if (shifted != null) {
                stopObs = shifted;
            }
        }
        float[] scores = new float[options.size()];
        long seed = ThreadLocalRandom.current().nextLong();
        float bestOfStop = logit(stopObs);
        if (horizonTurns > 0 && horizonStop) {
            // Same horizon, same determinizations, no action applied: the honest counterfactual of "do nothing now".
            float h = horizonScore(new MarsGame(game), player, seed);
            if (!Float.isNaN(h)) {
                bestOfStop = h;
            }
        }
        if (playoutTypes.contains(decision.type())) {
            // A playout candidate is compared against a playout of the same position: V(now) would put the one-action
            // afterstate and a whole played round on one scale, which is the mismatch this mode exists to avoid.
            float h = roundScore(new MarsGame(game), player, seed);
            if (!Float.isNaN(h)) {
                bestOfStop = Math.round(h * 1000f);
            }
        }
        int best = -1;
        float bestScore = Float.NEGATIVE_INFINITY;
        int failed = 0;
        int scored = 0;
        float[] bestAfterstate = null;
        float debugBest = Float.NEGATIVE_INFINITY;
        boolean batch = batchable(decision.type());
        float[][] mineObs = batch ? new float[options.size()][] : null;
        float[][] otherObs = batch ? new float[options.size()][] : null;
        for (int i = 0; i < options.size(); i++) {
            int id = options.get(i).id();
            if (id == stopId) {
                scores[i] = bestOfStop;
                scored++;
                continue;
            }
            if (batch) {
                float[][] prepared = prepare(game, player, id, decision.type(), seed);
                if (prepared == null) {
                    scores[i] = Float.NEGATIVE_INFINITY;
                    failed++;
                    continue;
                }
                mineObs[i] = prepared[0];
                otherObs[i] = prepared[1];
                scored++;
                continue;
            }
            Integer afterstate = score(game, player, id, decision.type(), seed);
            if (afterstate == null) {
                scores[i] = Float.NEGATIVE_INFINITY;
                failed++;
                continue;
            }
            scores[i] = shuffle ? ThreadLocalRandom.current().nextFloat() * 1000f : afterstate;
            scored++;
            if (debugThis && afterstate > debugBest) {
                debugBest = afterstate;
                bestAfterstate = lastAfterstate.get();
            }
        }
        if (debugThis && bestAfterstate != null) {
            reportDelta(stopObs, bestAfterstate);
        }
        if (batch) {
            scoreBatch(mineObs, otherObs, scores, bestOfStop);
        }
        if (failed > 0) {
            failures.addAndGet(failed);
        }
        if (scored == 0) {
            return -1;
        }
        // Two builds deep (synergies): for the two best cards by the single score, also score "A then the best other
        // card B" and keep the better of the two readings. A pair is a better estimate of a card's value than the card
        // alone when its worth is the chain it enables (one microbe that unlocks the next card), and every candidate is
        // still compared through the same quantity: the value of the position after the builds.
        if (pairs && !projectBuilds && rankThis && decision.type() == DecisionType.BUILD_CARD && options.size() > 2) {
            int[] top = new int[Math.min(2, options.size())];
            float[] topScores = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
            for (int i = 0; i < scores.length; i++) {
                if (options.get(i).id() == stopId || scores[i] == Float.NEGATIVE_INFINITY) {
                    continue;
                }
                if (scores[i] > topScores[0]) {
                    topScores[1] = topScores[0];
                    top[1] = top[0];
                    topScores[0] = scores[i];
                    top[0] = i;
                } else if (scores[i] > topScores[1]) {
                    topScores[1] = scores[i];
                    top[1] = i;
                }
            }
            for (int a = 0; a < top.length; a++) {
                for (int b = 0; b < top.length; b++) {
                    if (a == b) {
                        continue;
                    }
                    Integer pairScore = scorePair(game, player, options.get(top[a]).id(), options.get(top[b]).id(), seed);
                    if (pairScore != null && pairScore > scores[top[a]]) {
                        scores[top[a]] = pairScore;
                    }
                }
            }
        }
        int bestCard = -1;
        float bestCardScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < scores.length; i++) {
            if (options.get(i).id() != stopId && scores[i] > bestCardScore) {
                bestCardScore = scores[i];
                bestCard = i;
            }
        }
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > bestScore) {
                bestScore = scores[i];
                best = i;
            }
        }
        int policy = neuralPolicy.choose(decision, strongAnswer.get());
        // Ranking mode: V only re-ranks the cards, the policy keeps the build-or-stop decision. The afterstate of a build
        // is off the distribution V was trained on (a state between two decisions, building inside one action attempt),
        // so V(s) and V(after build) are not on one scale; card against card is a fair comparison.
        if (rankThis) {
            if (bestCard >= 0) {
                menuDecisions.incrementAndGet();
            }
            if (policy < 0 || options.get(policy).id() == BuildCardDecisions.STOP) {
                if (bestCard >= 0 && policy >= 0) {
                    policyStops.incrementAndGet();
                }
                // mustBuild: the diagnosis is that the policy HOARDS (it collects income and then declines ~40% of the
                // build opportunities), while the opponent that beats it converts every credit into cards. With this flag
                // the ranking decides even when the policy wants to stop: build the best card if one is available.
                float margin = stopMargins.getOrDefault(decision.type(), stopMargin);
                boolean marginSaysBuild = margin > 0f && bestCard >= 0
                        && bestCardScore - bestOfStop > margin;
                // "the removal is free, so the card is junk": the gap is zero (within float noise) rather than positive.
                boolean gated = junkGateTypes.contains(decision.type()) && bestCard >= 0;
                // The margin is a REQUIRED ADVANTAGE, not a tolerance: discard only when removing the card improves V by
                // more than the noise of the comparison. The gate compares the MAX over ~10 noisy candidates against one
                // baseline, and a maximum of ten noisy estimates is biased upward (measured mean +0.18 logit at zero true
                // effect), so a threshold of 0 fires almost always. A negative margin would accept a removal that costs
                // that much; the default 0 is the owner's literal rule "it costs me nothing".
                boolean freeRemoval = gated && bestCardScore >= bestOfStop + junkMargin;
                if (gated && debugJunk) {
                    long gap = Math.round(bestCardScore - bestOfStop);
                    junkSamples.incrementAndGet();
                    junkSum.addAndGet(gap);
                    if (freeRemoval) {
                        junkFree.incrementAndGet();
                    }
                    junkMin.accumulateAndGet(gap, Math::min);
                    junkMax.accumulateAndGet(gap, Math::max);
                }
                if ((!mustBuild && !marginSaysBuild && !freeRemoval) || bestCard < 0) {
                    return policy;
                }
            }
            if (bestCard < 0) {
                return policy;
            }
            // The critic re-orders the menu, but for the hand menus the *policy's* candidate is kept: it is the only one of
            // the two that can see which card is which. The critic still had its say above - it decided that acting is
            // worth it at all.
            best = policyChoiceTypes.contains(decision.type()) ? policy : bestCard;
        }
        decisions.incrementAndGet();
        candidates.addAndGet(scored);
        if (options.get(best).id() == stopId && options.size() > 1) {
            stops.incrementAndGet();
        }
        float bestBuild = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).id() != stopId && scores[i] > bestBuild) {
                bestBuild = scores[i];
            }
        }
        if (bestBuild > Float.NEGATIVE_INFINITY) {
            gainSum.addAndGet(Math.round((bestBuild - bestOfStop) * 1000f));
        }
        if (policy != best) {
            disagreements.incrementAndGet();
        }
        // The ranking is a policy-improvement target: the trainer can distil the whole ordering (all candidates) instead
        // of only the card that was taken. Recorded by DecisionLogs when it writes this decision's row.
        if (!models.isEmpty() && (decision.type() == DecisionType.BUILD_CARD
                || decision.type() == DecisionType.DRAFT_CARDS)) {
            SearchScores.put(decision, scores.clone());
        }
        return best;
    }

    /**
     * Scores a whole menu in one pass per critic: the candidates' afterstate observations are collected first and then
     * evaluated together, so the weight block is read once instead of once per candidate (see
     * {@link MlpValueModel#batchLogits}). Returns null when the menu cannot be batched (a path that needs playouts).
     */
    private boolean batchable(DecisionType type) {
        return BATCH
                && horizonTurns == 0 && !phasePlayout && !projectBuilds && !shuffle
                && type != DecisionType.PICK_PHASE;
    }

    /** The afterstate observations of one candidate: {mine, opponent's public view or null}. */
    private float[][] prepare(MarsGame game, Player player, int cardId, DecisionType type, long seed) {
        try {
            MarsGame copy = new MarsGame(game);
            Player me = copy.getPlayerByUuid(player.getUuid());
            if (!applyOn(copy, me, cardId, type, seed)) {
                return null;
            }
            float[] mine = observationBuilder.build(copy, me);
            float[] other = null;
            if (symmetric) {
                Player opponent = otherPlayer(copy, me);
                if (opponent != null) {
                    other = observationBuilder.buildPublic(copy, opponent);
                }
            }
            return new float[][]{mine, other};
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The menu scored in batch: one forward pass per critic over all usable candidates. */
    private void scoreBatch(float[][] mine, float[][] other, float[] scores, float stopScore) {
        List<MlpValueModel> nets = nets();
        int count = 0;
        for (float[] obs : mine) {
            if (obs != null) {
                count++;
            }
        }
        if (count == 0) {
            return;
        }
        float[][] compactMine = new float[count][];
        float[][] compactOther = new float[count][];
        int[] slot = new int[count];
        int at = 0;
        for (int i = 0; i < mine.length; i++) {
            if (mine[i] != null) {
                compactMine[at] = mine[i];
                compactOther[at] = other[i];
                slot[at] = i;
                at++;
            }
        }
        for (MlpValueModel model : nets) {
            float[] mineLogits = model.batchLogits(compactMine, count);
            float[] otherLogits = null;
            boolean needOther = symmetric && compactOther[0] != null;
            if (needOther) {
                otherLogits = model.batchLogits(compactOther, count);
            }
            for (int k = 0; k < count; k++) {
                float value = needOther ? (mineLogits[k] - otherLogits[k]) * 0.5f : mineLogits[k];
                // The one-candidate path returns Math.round(V * 1000) and the pair logic compares raw numbers, so the
                // batch path must use the same scale - mixing them silently made every pair outrank every batch score
                // (measured: 8.4% instead of 44%, 2026-11-16).
                scores[slot[k]] += Math.round(value * 1000f) / (float) nets.size();
            }
        }
    }

    /** Applies one candidate's effect to a copy. Divided out of {@link #score} so the batch path can reuse it. */
    private boolean applyOn(MarsGame copy, Player me, int id, DecisionType type, long seed) {
        Card candidate = type == DecisionType.STANDARD_ACTION ? null : cardService.getCard(id);
        if (type == DecisionType.STANDARD_ACTION) {
            switch (id) {
                case StandardActionDecisions.EXCHANGE_ONE_HEAT -> aiTurnService.exchangeHeat(copy, me, 1);
                case StandardActionDecisions.PLANT_FOREST -> aiTurnService.plantForest(copy, me);
                case StandardActionDecisions.INCREASE_TEMPERATURE -> aiTurnService.increaseTemperature(copy, me);
                case StandardActionDecisions.SP_OCEAN -> aiTurnService.standardProjectTurn(copy, me, StandardProjectType.OCEAN);
                case StandardActionDecisions.SP_FOREST -> aiTurnService.standardProjectTurn(copy, me, StandardProjectType.FOREST);
                case StandardActionDecisions.SP_TEMPERATURE ->
                        aiTurnService.standardProjectTurn(copy, me, StandardProjectType.TEMPERATURE);
                default -> {
                    return false;
                }
            }
            return true;
        }
        if (type == DecisionType.PERFORM_BLUE_ACTION) {
            Map<Integer, List<Integer>> input = blueInputDecisions.fill(copy, me, candidate, this::innerChoice);
            aiTurnService.performBlueAction(copy, me, id, input);
            return true;
        }
        if (type == DecisionType.MULLIGAN) {
            // The true counterfactual of the k-th discard: the cards already queued by the loop leave the hand too, and the
            // engine deals as many replacements as the whole set. Modelling only the candidate kept the queued cards in the
            // copy's hand, so every step looked as good as the first.
            applyMulligan(me, copy, id);
            return true;
        }
        if (type == DecisionType.UNMI_RT) {
            // +1 TR for 6 MC: no afterstate of its own beyond the TR and the money, so the sync turn is the whole effect.
            aiTurnService.unmiRtCorporationTurnSync(copy, me);
            return true;
        }
        if (type == DecisionType.COLLECT_INCOME) {
            // The phase-4 double-produce: pay the chosen green card's income a second time. Applied through the engine's
            // own processor, because the turn itself is asynchronous and would never run on a copy.
            collectIncomeTurnProcessor.processTurn(
                    new com.terraforming.ares.model.turn.CollectIncomeTurn(me.getUuid(), id), copy);
            return true;
        }
        if (isGiveUpCard(type)) {
            me.getHand().removeCard(id);
            return true;
        }
        if (type == DecisionType.PICK_PHASE) {
            applyPhaseChoice(copy, me, id, seed);
            return true;
        }
        if (type == DecisionType.BUILD_CARD) {
            applyBuild(copy, me, id);
            return true;
        }
        return false;
    }

    /** Applies one candidate to a copy and returns V(afterstate) as a logit, or null when the move is not applicable. */
    private Integer score(MarsGame game, Player player, int cardId, DecisionType type, long seed) {
        try {
            MarsGame copy = new MarsGame(game);
            Player me = copy.getPlayerByUuid(player.getUuid());
            // ONE dispatcher for both paths. The second copy of this switch was the bug found on 18/11: a type added to
            // applyOn alone (MULLIGAN) scored as "no opinion" here, so the mulligan menu answered stop in 100% of rows, and
            // UNMI_RT / COLLECT_INCOME were missing here for the same reason. The batch path was slower and is off by
            // default, so this one is the live one - it must not be the less complete one.
            if (!applyOn(copy, me, cardId, type, seed)) {
                return null;
            }
            if (projectBuilds && (type == DecisionType.BUILD_CARD || type == DecisionType.STANDARD_ACTION)) {
                // Read the value at the player's next decision instead of at the mid-turn afterstate.
                float projected = projectToNextDecision(copy, me, seed);
                return Float.isNaN(projected) ? null : Math.round(projected * 1000f);
            }
            float[] observation = observationBuilder.build(copy, me);
            lastAfterstate.set(observation);
            float mine;
            if (playoutTypes.contains(type)) {
                mine = roundScore(copy, me, seed);
                if (Float.isNaN(mine)) {
                    return null;
                }
            } else if (horizonTurns > 0) {
                mine = horizonScore(copy, me, seed);
            } else if (phasePlayout && type == DecisionType.PICK_PHASE) {
                mine = roundScore(copy, me, seed);
            } else {
                mine = valueAt(copy, me, observation);
            }
            if (type == DecisionType.PICK_PHASE) {
                mine += phaseBias.getOrDefault(cardId, 0f);
            }
            return Math.round(mine * 1000f);
        } catch (RuntimeException e) {
            boolean report = (debug > 0 && failures.get() < 8
                    && (type == DecisionType.STANDARD_ACTION || type == DecisionType.PICK_PHASE))
                    || (debugJunk && junkGateTypes.contains(type) && failures.get() < 4);
            if (report) {
                failures.incrementAndGet();
                System.out.println("[SEARCH-DEBUG] " + type + " " + cardId + " failed: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace(System.out);
            }
            return null;
        }
    }


    /** Thrown from the per-step callback to stop a playout at the horizon; never leaves the search thread. */
    private static final class StopPlayout extends RuntimeException {
        StopPlayout() {
            super(null, null, false, false);
        }
    }

    /** Decisions whose candidate is a card that leaves the hand: draft, discard, selling. */
    /**
     * The mulligan's "stop" baseline: the hand with the queued cards already replaced (the engine does this when the player
     * says stop), so the gate compares two hands of the same size and content shape.
     */
    private float[] mulliganStopObservation(MarsGame game, Player player) {
        try {
            MarsGame copy = new MarsGame(game);
            Player me = copy.getPlayerByUuid(player.getUuid());
            List<Integer> queued = mulliganQueued.get();
            for (int id : queued) {
                me.getHand().removeCard(id);
            }
            List<Integer> draws = mulliganDraws.get();
            me.getHand().addCards(draws.subList(0, Math.min(queued.size(), draws.size())));
            // The baseline must found a corporation too: a candidate afterstate that has one and a stop baseline that does
            // not differ by the corporation's whole starting bonus, and every discard would look free.
            applyOfferedCorporations(copy, me);
            return observationBuilder.build(copy, me);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The replacement cards of this decision, drawn from a copy so the real deck is untouched. */
    private List<Integer> drawMulliganCards(MarsGame game, int count) {
        if (count <= 0) {
            return List.of();
        }
        try {
            MarsGame copy = new MarsGame(game);
            return List.copyOf(cardService.dealCards(copy, count));
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /**
     * The mulligan's afterstate: the hand without the queued cards and the candidate, plus as many random replacements as
     * the whole set. The engine applies exactly this when the player stops.
     */
    private void applyMulligan(Player me, MarsGame copy, int candidateId) {
        List<Integer> queued = mulliganQueued.get();
        for (int id : queued) {
            me.getHand().removeCard(id);
        }
        me.getHand().removeCard(candidateId);
        List<Integer> draws = mulliganDraws.get();
        if (draws.size() >= queued.size() + 1) {
            me.getHand().addCards(draws);
        } else {
            // No sampled set (a decision that never went through search): fall back to the engine's own draw.
            me.getHand().addCards(cardService.dealCards(copy, queued.size() + 1));
        }
        applyOfferedCorporations(copy, me);
    }

    /**
     * The mulligan is decided while the two corporations are already on the table: `PickCorporationsState` offers
     * `PICK_CORPORATION` and `MULLIGAN` together (the owner's description of how a player does it, 19/11 - look at the two
     * corporations, throw away what fits neither, then take the one that fits the hand).
     * <p>
     * So the value of a hand is the value of the hand WITH the best of the two corporations, not of a hand alone: without this
     * the afterstate is a position the engine never reaches (a player who never founds a corporation), and the comparison
     * cannot express "this card fits corporation A".
     */
    private void applyOfferedCorporations(MarsGame copy, Player me) {
        applyOfferedCorporation(copy, me);
        Player other = otherPlayer(copy, me);
        if (other != null) {
            // The other seat's corporation is still unchosen in the copy, and a rollout would ask the policy for it - the
            // policy does not fill a corporation's on-built inputs and the engine throws ("Corporation X applies an on-built
            // effect to itself, but the legal inputs..."). Founding it from that seat's own two offers is the same kind of
            // determinization the search already does for hidden cards: the position has to be playable for both seats.
            applyOfferedCorporation(copy, other);
        }
    }

    private void applyOfferedCorporation(MarsGame copy, Player me) {
        if (me.getSelectedCorporationCard() != null) {
            return;
        }
        List<CorporationChoice> choices;
        try {
            choices = corporationDecisions.options(copy, me);
        } catch (RuntimeException e) {
            return;
        }
        if (choices.isEmpty()) {
            return;
        }
        int best = -1;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < choices.size(); i++) {
            try {
                MarsGame probe = new MarsGame(copy);
                Player seat = probe.getPlayerByUuid(me.getUuid());
                CorporationChoice choice = choices.get(i);
                pickCorporationProcessor.processTurn(new com.terraforming.ares.model.turn.CorporationChoiceTurn(
                        me.getUuid(), choice.corporationId(), choice.input()), probe);
                settleQueuedDiscards(probe, seat);
                float score = logit(observationBuilder.build(probe, seat));
                if (score > bestScore) {
                    bestScore = score;
                    best = i;
                }
            } catch (RuntimeException ignored) {
                // a corporation the engine rejects on this hand is not a candidate for this hand
            }
        }
        if (best < 0) {
            return;
        }
        CorporationChoice choice = choices.get(best);
        pickCorporationProcessor.processTurn(new com.terraforming.ares.model.turn.CorporationChoiceTurn(
                me.getUuid(), choice.corporationId(), choice.input()), copy);
        settleQueuedDiscards(copy, me);
        mulliganCorps.incrementAndGet();
    }

    /** {@code "1:0.3,5:-0.1"} - phase number to logit handicap. Unknown numbers are a hard error, never ignored. */
    private static java.util.Map<Integer, Float> parsePhaseBias(String spec) {
        if (spec == null || spec.isBlank()) {
            return java.util.Map.of();
        }
        java.util.Map<Integer, Float> parsed = new java.util.HashMap<>();
        for (String part : spec.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                throw new IllegalArgumentException("phaseBias entry must look like PHASE:LOGITS, got '" + trimmed + "'");
            }
            int phase = Integer.parseInt(trimmed.substring(0, colon).trim());
            if (phase < 1 || phase > 5) {
                throw new IllegalArgumentException("phaseBias phase out of 1..5: '" + trimmed + "'");
            }
            parsed.put(phase, Float.parseFloat(trimmed.substring(colon + 1).trim()));
        }
        return java.util.Map.copyOf(parsed);
    }

    private static boolean isGiveUpCard(DecisionType type) {
        return type == DecisionType.DRAFT_CARDS || type == DecisionType.DISCARD_CARDS
                || type == DecisionType.SELL_CARDS || type == DecisionType.SELL_CARDS_LAST_ROUND;
    }

    /**
     * Plays the position forward {@link #horizonTurns} generations under the policy and reads {@code V} at the leaf.
     * <p>
     * Fairness: every sample starts from a <b>determinization</b> - the cards I cannot see (the opponent's hand and
     * corporations, both decks) are shuffled among their own slots, keeping only the multiset, which is public
     * (everything that has been played or discarded is known). The playout therefore never conditions on the true hidden
     * cards, and the average over {@link #samples} draws is an estimate a real player could compute.
     */
    private float horizonScore(MarsGame copy, Player me, long seed) {
        String uuid = me.getUuid();
        float sum = 0f;
        int ok = 0;
        for (int k = 0; k < samples; k++) {
            MarsGame sim = k == 0 ? copy : new MarsGame(copy);
            // Common random numbers: the seed depends on the decision and the sample index, never on the candidate, so
            // every candidate is judged on the SAME determinization. Without it the comparison is dominated by which
            // hidden cards the sample happened to deal, not by the card being scored.
            Determinizer.shuffle(sim, uuid, seed + k);
            float leaf = playout(sim, uuid);
            if (!Float.isNaN(leaf)) {
                sum += leaf;
                ok++;
            }
        }
        return ok == 0 ? Float.NaN : sum / ok;
    }

    /**
     * Plays the round out from the afterstate of a phase pick and reads V at the next pick: every candidate has collected
     * what its phase pays (a build, income, the draft, blue actions) before the leaf is read, so the comparison is between
     * complete rounds rather than between the phases' opening gifts.
     */
    private float roundScore(MarsGame copy, Player me, long seed) {
        String uuid = me.getUuid();
        // A playout needs both seats to be playable by the engine; against a human the search simply has no opinion and
        // the policy decides (the same fallback as an unusable candidate).
        if (copy.getPlayerUuidToPlayer().values().stream().anyMatch(player -> !player.isComputer())) {
            return Float.NaN;
        }
        float sum = 0f;
        int ok = 0;
        for (int k = 0; k < samples; k++) {
            MarsGame sim = k == 0 ? copy : new MarsGame(copy);
            Determinizer.shuffle(sim, uuid, seed + k);
            float leaf = playRound(sim, uuid);
            if (!Float.isNaN(leaf)) {
                sum += leaf;
                ok++;
            }
        }
        return ok == 0 ? Float.NaN : sum / ok;
    }

    private float playRound(MarsGame sim, String uuid) {
        int startTurns = sim.getTurns();
        try {
            simulationProcessorService.processSimulation(sim, game -> {
                if (game.getStateType() == StateType.GAME_END
                        || (game.getTurns() > startTurns && game.getStateType() == StateType.PICK_PHASE)) {
                    throw new StopPlayout();
                }
            });
        } catch (StopPlayout stop) {
            // the round is over: the leaf is the next pick
        } catch (RuntimeException e) {
            return Float.NaN;
        }
        return leafValue(sim, uuid);
    }

    private float playout(MarsGame sim, String uuid) {
        int target = sim.getTurns() + horizonTurns;
        try {
            simulationProcessorService.processSimulation(sim, game -> {
                if (game.getTurns() >= target || game.getStateType() == StateType.GAME_END) {
                    throw new StopPlayout();
                }
            });
        } catch (StopPlayout stop) {
            // the horizon was reached: the state is the leaf
        } catch (RuntimeException e) {
            return Float.NaN;
        }
        return leafValue(sim, uuid);
    }

    /** Terminal result when the playout reached the end, {@code V} of the position otherwise. */
    private float leafValue(MarsGame sim, String uuid) {
        Player mine = sim.getPlayerByUuid(uuid);
        Player other = otherPlayer(sim, mine);
        if (sim.getStateType() == StateType.GAME_END && other != null) {
            int margin = winPointsService.countWinPoints(mine, sim) - winPointsService.countWinPoints(other, sim);
            // 10 points of margin is a comfortable game; the scale is only used to rank candidates of one decision.
            return Math.max(-6f, Math.min(6f, margin / 10f));
        }
        float[] observation = observationBuilder.build(sim, mine);
        if (!symmetric || other == null) {
            return logit(observation);
        }
        return symmetricScore(observation, observationBuilder.buildPublic(sim, other));
    }

    /**
     * Shuffles the hidden part of the position: the opponent's hand and corporations and both decks. My own hand stays
     * mine, the played and discarded cards stay where they are - only the assignment of the unseen cards is randomized,
     * which is exactly the information a fair player does not have.
     */
    /**
     * V of a position, symmetric when configured: my own reading and the opponent's mirror of the same public state. The
     * opponent's observation carries no hidden information, so this is a fair, less noisy estimate of the position.
     */
    /**
     * Applies the candidate and plays the game on (both seats, the policy deciding) until this player is asked again or
     * the round ends, then reads V there. This is the cheap version of what NETWORK's {@code projectPhasePick +
     * assumeProjectIsBuilt} did: the leaf is a decision state, not a mid-turn afterstate.
     */
    private float projectToNextDecision(MarsGame copy, Player me, long seed) {
        String uuid = me.getUuid();
        Determinizer.shuffle(copy, uuid, seed);
        int startTurns = copy.getTurns();
        int startStep = copy.getTurns();
        try {
            simulationProcessorService.processSimulation(copy, game -> {
                if (game.getStateType() == StateType.GAME_END) {
                    throw new StopPlayout();
                }
                // One decision of this player inside the same round is enough: the point is to leave the mid-turn state.
                if (game.getTurns() > startTurns || game.getCurrentPhase() != copy.getCurrentPhase()) {
                    throw new StopPlayout();
                }
            });
        } catch (StopPlayout stop) {
            // reached the leaf
        } catch (RuntimeException e) {
            return Float.NaN;
        }
        Player leaf = copy.getPlayerByUuid(uuid);
        if (leaf == null) {
            return Float.NaN;
        }
        return valueAt(copy, leaf, observationBuilder.build(copy, leaf));
    }

    private float valueAt(MarsGame copy, Player me, float[] observation) {
        if (!symmetric) {
            return logit(observation);
        }
        Player other = otherPlayer(copy, me);
        return other == null ? logit(observation) : symmetricScore(observation, observationBuilder.buildPublic(copy, other));
    }

    /**
     * The afterstate of one phase pick: exactly what the engine does when the pick is confirmed. It is not re-implemented
     * here - {@link PickPhaseProcessor} is called, the same bean the engine uses ({@code clearRoundResults}, the chosen and
     * previous phase, the phase-3 double-repeat upgrade, Communications Streamlining and Nebu Labs MC, the crysis
     * cleanup), and then {@link StateTransitionService#performStateTransferFromPhase} with 0, which is what
     * {@code PickPhaseState} calls: the phase's build slots, income, draft, revealed cards and blue activations are set up
     * by the engine itself, so the afterstate cannot drift away from the rules.
     * <p>
     * The opponent's pick is unknown while we pick, so it is drawn from that player's legal phases with the decision's own
     * seed: the same draw for every candidate (common random numbers), so the comparison stays fair, and neither a hidden
     * card nor the opponent's hand can enter the value.
     */
    private void applyPhaseChoice(MarsGame copy, Player me, int phaseId, long seed) {
        pickPhaseProcessor.processTurn(new PhaseChoiceTurn(me.getUuid(), phaseId), copy);
        Player other = otherPlayer(copy, me);
        if (other != null && other.getChosenPhase() == null) {
            List<Integer> legal = PickPhaseDecisions.legalPhases(other);
            if (!legal.isEmpty()) {
                int pick = legal.get((int) Math.floorMod(seed, legal.size()));
                pickPhaseProcessor.processTurn(new PhaseChoiceTurn(other.getUuid(), pick), copy);
            }
        }
        stateTransitionService.performStateTransferFromPhase(copy, 0);
    }

    /** Applies one build on the copy exactly as the live game does (sync variants), including its inputs and payment. */
    private void applyBuild(MarsGame copy, Player me, int cardId) {
        Card card = cardService.getCard(cardId);
        Map<Integer, List<Integer>> input = buildInputDecisions.fill(copy, me, card, this::innerChoice);
        List<BuildPaymentChoice> payments = buildPaymentDecisions.options(copy, me, card, input);
        int payment = innerChoice(buildPaymentDecisions.decision(copy, me, card, payments));
        // The live game queues the build as a turn (makeAsyncTurn) and the engine applies it later; on a copy there is no
        // engine pass, so the SYNC variant applies the same effects immediately.
        if (card.getColor() == com.terraforming.ares.model.CardColor.GREEN) {
            aiTurnService.buildGreenProjectSync(copy, me, cardId, payments.get(payment).payments(), input);
        } else {
            aiTurnService.buildBlueRedProjectSync(copy, me, cardId, payments.get(payment).payments(), input);
        }
    }

    /** Two builds on one copy: the afterstate of "A then B", which is what a synergy chain is worth. */
    private Integer scorePair(MarsGame game, Player player, int firstId, int secondId, long seed) {
        try {
            MarsGame copy = new MarsGame(game);
            Player me = copy.getPlayerByUuid(player.getUuid());
            applyBuild(copy, me, firstId);
            applyBuild(copy, me, secondId);
            return Math.round(valueAt(copy, me, observationBuilder.build(copy, me)) * 1000f);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Player otherPlayer(MarsGame game, Player me) {
        for (Player candidate : game.getPlayerUuidToPlayer().values()) {
            if (!candidate.getUuid().equals(me.getUuid())) {
                return candidate;
            }
        }
        return null;
    }

    /** A re-entrant call (input / payment of the simulated build) is answered by the policy, not by the search. */
    private int innerChoice(Decision decision) {
        int index = neuralPolicy.choose(decision, strongAnswer.get());
        return index < 0 ? 0 : index;
    }

    private static double sigmoid(float logit) {
        return 1.0 / (1.0 + Math.exp(-logit));
    }

    /** Diagnostic: which observation features move when a card is built (the largest changes first). */
    private synchronized void reportDelta(float[] before, float[] after) {
        List<String> names;
        try {
            names = ObservationBuilder.names();
        } catch (RuntimeException e) {
            names = List.of();
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < Math.min(before.length, after.length); i++) {
            order.add(i);
        }
        order.sort((x, y) -> Float.compare(Math.abs(after[y] - before[y]), Math.abs(after[x] - before[x])));
        System.out.printf(Locale.ROOT, "[SEARCH-DEBUG] V(before) = %.4f, V(best afterstate) = %.4f%n",
                sigmoid(logit(before)), sigmoid(logit(after)));
        for (int k = 0; k < Math.min(12, order.size()); k++) {
            int i = order.get(k);
            if (before[i] == after[i]) {
                break;
            }
            System.out.printf(Locale.ROOT, "[SEARCH-DEBUG]   %-55s %+.3f -> %+.3f%n",
                    i < names.size() ? names.get(i) : ("#" + i), before[i], after[i]);
        }
    }

    private void report() {
        if (models.isEmpty() || decisions.get() == 0) {
            return;
        }
        long n = decisions.get();
        if (debugJunk && junkSamples.get() > 0) {
            System.out.printf(Locale.ROOT, "[JUNK] %d mulligan afterstates founded a corporation; %d gated decisions, "
                            + "free %d (%.1f%%), gap min %d mean %.1f max %d milli%n",
                    mulliganCorps.get(),
                    junkSamples.get(), junkFree.get(), 100.0 * junkFree.get() / junkSamples.get(),
                    junkMin.get(), (double) junkSum.get() / junkSamples.get(), junkMax.get());
        }
        System.out.println(String.format(Locale.ROOT,
                "[SEARCH] %d decisions, %.1f candidates each, %.2f ms per decision, %.1f%% of the choices differ "
                        + "from the policy, %.1f%% chose stop, mean V(best build) - V(stop) = %+.3f logit, "
                        + "%d unusable candidates",
                n, candidates.get() / (double) n, nanos.get() / 1e6 / n,
                100.0 * disagreements.get() / n, 100.0 * stops.get() / n, gainSum.get() / 1000.0 / n,
                failures.get()));
        if (menuDecisions.get() > 0) {
            // The headroom this search can not touch: a decision that offered cards while the policy declined to build.
            System.out.println(String.format(Locale.ROOT,
                    "[SEARCH] %d decisions offered a card to build, the policy declined %d of them (%.1f%%)",
                    menuDecisions.get(), policyStops.get(), 100.0 * policyStops.get() / menuDecisions.get()));
        }
    }
}
