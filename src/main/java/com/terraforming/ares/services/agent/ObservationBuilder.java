package com.terraforming.ares.services.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.parameters.Ocean;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.cards.blue.FilterFeeders;
import com.terraforming.ares.cards.blue.MatterGenerator;
import com.terraforming.ares.cards.blue.PhysicsComplex;
import com.terraforming.ares.cards.blue.FibrousCompositeMaterial;
import com.terraforming.ares.cards.blue.Pets;
import com.terraforming.ares.cards.blue.Sawmill;
import com.terraforming.ares.cards.green.ArchitectureBlueprints;
import com.terraforming.ares.cards.green.BedrockWellbore;
import com.terraforming.ares.cards.green.CityPlanning;
import com.terraforming.ares.cards.green.GrainSilos;
import com.terraforming.ares.cards.green.InvasiveIrrigation;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.advanced.CompleteHandEncoder;
import com.terraforming.ares.services.ai.advanced.CompleteTableEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The observation of the learnable agent ("v3"): everything a player is allowed to know, in one vector, for EVERY decision.
 * <pre>
 *   [ V2 encoder without its exact copies (688) | round context (CONTEXT_SIZE) | previous phases and the dummy pool (17) ]
 * </pre>
 * <ul>
 *   <li><b>V2 encoder</b> (stable, already used by NETWORK_V2) describes the state between the rounds: parameters, both tables, incomes, tags,
 *       resources on cards, upgrades, milestones, own hand. 210 of its 898 columns are exact copies of other columns (the hand vector re-encodes
 *       the table); they are dropped by the mask {@code observation/v3_mask.json}, tested to stay exact copies.</li>
 *   <li><b>round context</b>: what the encoder does not know because it only exists inside a round - which phases were picked (by me, by the
 *       opponent and by the dummy), which phase is being played, builds still available, extra blue activations, draft modifiers, activated blue cards.
 *       Picks are HIDDEN while the phase is being picked (simultaneous choice), the stale values of the previous round must not leak in.</li>
 *   <li><b>extras</b>: round number, previous phases, the SET of phases left in the dummy pool (its order is hidden).</li>
 * </ul>
 * Changing any of it changes {@link #LAYOUT} and with it the schema hash: data and models of another layout are refused.
 */
@Service
@RequiredArgsConstructor
public class ObservationBuilder {
    public static final int PHASES = 5;
    /**
     * round context: revealed flag, current phase (5) + sell flag, picks of me / opponent / dummy (15), builds, activations, ...
     */
    public static final int CONTEXT_SIZE = 1 + 5 + 1 + 15 + 2 * 2 + 2 + 2 + 2 + 2;
    /**
     * The score of each player, one number, from the engine's own total: printed card points, points on animal and
     * microbe cards, terraforming, forests, milestones and awards are all the same thing to the value function (owner's
     * rule). The V2 block keeps its old, legacy `win_points`, because NETWORK_V2 is trained on it.
     */
    public static final int SCORE_SIZE = 2;
    public static final int EXTRAS_SIZE = 1 + PHASES + PHASES + PHASES + 1;

    /**
     * Cards the V2 encoder can not tell apart (found by CardTrackingAuditTest; the V2 encoder must not be changed - NETWORK_V2 is trained on it):
     * blue cards with an action or a resource that matter when they are on the table, and newest cards that the hand encoder does not know.
     */
    /**
     * The expansions this player is trained and played on. A card outside this set can never be built (its deck is not in
     * the game), so a bit for it would never be set: dead columns dilute the vector and hide bugs (owner's audit rule).
     */
    public static final Set<Expansion> PLAYED_EXPANSIONS =
            Set.of(Expansion.BASE, Expansion.BUFFED_CORPORATION, Expansion.DISCOVERY);

    /** Table cards the legacy V2 encoder cannot tell apart - only the ones that exist in the played expansions. */
    static final List<Class<? extends Card>> TABLE_CARDS = List.of(MatterGenerator.class, FilterFeeders.class);
    /**
     * Cards that hold SCIENCE. PhysicsComplex is worth one point per two science, so the count matters the way an animal
     * count does - it was the one resource type with no counter at all. CommunicationCenter holds science too but belongs
     * to the EXPERIMENTAL expansion, which this player never plays, so it is not here.
     */
    static final List<Class<? extends Card>> SCIENCE_CARDS = List.of(PhysicsComplex.class, FibrousCompositeMaterial.class);
    /**
     * Empty on purpose: the five cards this list used to carry (BedrockWellbore, ArchitectureBlueprints, CityPlanning,
     * GrainSilos, InvasiveIrrigation) all belong to the INFRASTRUCTURE expansion, which is never played here.
     */
    static final List<Class<? extends Card>> HAND_CARDS = List.of();
    public static final int CARDS_SIZE = 2 * TABLE_CARDS.size() + 2 * SCIENCE_CARDS.size() + HAND_CARDS.size();

    /**
     * Phase 4 can make ONE played card pay again, and 130 cards can do that — flagging them all would cost 260 columns
     * for information that is only ever used as "what is the best single double payment available". So the state carries
     * the maximum double payment per dimension instead (owner's design, 2026-11-14): the best MC a card can pay again,
     * the best plants, heat, cards, TR and each income. It is computed by actually playing {@code payAgain} on a copy for
     * the few such cards the player owns, so the dynamic ones ("this card pays 3 science") need no table.
     */
    static final List<String> DOUBLE_PAYMENT_DIMENSIONS = List.of(
            "mc", "plants", "heat", "cards", "tr", "mc_income", "plants_income", "heat_income", "card_income",
            "steel_income", "titanium_income");
    public static final int DOUBLE_PAYMENT_SIZE = 2 * DOUBLE_PAYMENT_DIMENSIONS.size();

    /**
     * Terraforming can still be advanced in the CURRENT phase even when a global is already maxed: the engine checks
     * `planetAtTheStartOfThePhase` (`TerraformingService.canIncreaseTemperature`), not the current planet. That is what
     * makes "hold Livestock, dump 300 MC into temperature in phase 3, one animal per step" work - and without these four
     * bits the state cannot tell "temperature is maxed and closed" from "temperature is maxed but this phase still pays
     * TR and every onTemperatureChangedEffect". One bit per global, per game.
     */
    static final List<String> PHASE_START_DIMENSIONS = List.of("temperature", "oxygen", "oceans");
    public static final int PHASE_START_SIZE = PHASE_START_DIMENSIONS.size() + 2;

    /**
     * The ocean deck is a fixed multiset of rewards, shuffled over 9 slots. Once all nine are revealed, `revealOcean()`
     * returns the LAST opened one (`lastOpenedOceanIndex`), so every further ocean purchase in that phase pays the same
     * reward - a 13 MC ocean that pays back 4 MC is a 9 MC ocean, repeatedly. The state therefore carries the reward of
     * the last opened ocean, the totals collected so far and how many are open; the positions themselves are a shuffle
     * and carry no information.
     */
    static final List<String> OCEAN_DIMENSIONS = List.of("cards", "mc", "plants");
    public static final int OCEAN_SIZE = 2 * OCEAN_DIMENSIONS.size() + 1;

    public static final String LAYOUT = "ObservationBuilder v3.1: V2 encoder (table 417 + hand 481) without 210 exact copies (observation/v3_mask.json)"
            + " + roundContext[revealed, currentPhase(5), sellFlag, picks me/opponent/dummy (15), builds me/opponent (8), extra blue activations (2),"
            + " luna office (2), project inspection (2), activated blue cards (2)]"
            + " + cards[on table me/opponent: MatterGenerator FilterFeeders (4),"
            + " science on PhysicsComplex FibrousCompositeMaterial me/opponent (4)]"
            + " + score[winning points of me/opponent: ONE number, every source of points is the same point]"
            + " + extras[round/40, myPreviousPhase(5), opponentPreviousPhase(5), dummyPoolMembership(5), dummyPoolSize/5]"
            + " + doublePayment[best single phase-4 repeat per dimension, me/opponent (22): mc, plants, heat, cards, tr,"
            + " and the six incomes /10]"
            + " + rules[globals still advanceable this phase from planetAtTheStartOfThePhase (4: temperature, oxygen,"
            + " infrastructure, oceans), special-design-last-turn me/opponent (2)]";

    private static final Set<StateType> PICKS_ARE_KNOWN = Set.of(StateType.BUILD_GREEN_PROJECTS, StateType.BUILD_BLUE_RED_PROJECTS,
            StateType.PERFORM_BLUE_ACTION, StateType.COLLECT_INCOME, StateType.DRAFT_CARDS, StateType.SELL_EXTRA_CARDS);

    private static final int[] KEEP = dropLegacy(loadMask());

    private final AdvancedAiDataCollectionService baseEncoder;
    private final CardService cardService;
    private final WinPointsService winPointsService;

    private static int[] loadMask() {
        try (InputStream in = ObservationBuilder.class.getResourceAsStream("/observation/v3_mask.json")) {
            if (in == null) {
                throw new IllegalStateException("observation/v3_mask.json is missing");
            }
            JsonNode mask = new ObjectMapper().readTree(in);
            if (mask.get("baseSize").asInt() != AiConstants.TOTAL_SIZE) {
                throw new IllegalStateException("The mask was made for " + mask.get("baseSize").asInt() + " columns, the V2 encoder produces "
                        + AiConstants.TOTAL_SIZE + ": regenerate it with ml/audit_features.py --write-mask");
            }
            int[] keep = new int[mask.get("keep").size()];
            for (int i = 0; i < keep.length; i++) {
                keep[i] = mask.get("keep").get(i).asInt();
            }
            return keep;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Indices (in the V2 vector) of the columns that stay.
     */
    /**
     * The mask without three legacy columns that carry nothing the kept ones do not: the "hand" copy of the table block
     * wrote log1p(mcIncome), log1p(heat) and log1p(heatIncome) again, which the duplicate filter could not see because of
     * the log. A local set, because this runs while the class is being initialised.
     */
    private static int[] dropLegacy(int[] keep) {
        java.util.Set<String> dropped = java.util.Set.of(
                "hand:mcincome_log", "hand:heat_log", "hand:heat_income_log");
        List<String> v2 = new ArrayList<>(CompleteTableEncoder.getAllFeatureNames());
        for (String name : CompleteHandEncoder.getAllFeatureNames()) {
            v2.add("hand:" + name);
        }
        List<Integer> filtered = new ArrayList<>(keep.length);
        for (int column : keep) {
            if (!dropped.contains(v2.get(column))) {
                filtered.add(column);
            }
        }
        int[] result = new int[filtered.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = filtered.get(i);
        }
        return result;
    }

    /** Index of the first value of the extras block (round, previous phases, dummy pool). Never hardcode offsets. */
    public static int extrasOffset() {
        return names().indexOf("extra:round/40");
    }

    public static int[] keptColumns() {
        return KEEP.clone();
    }

    public static int size() {
        return KEEP.length + CONTEXT_SIZE + CARDS_SIZE + SCORE_SIZE + EXTRAS_SIZE + DOUBLE_PAYMENT_SIZE
                + PHASE_START_SIZE + OCEAN_SIZE;
    }

    /**
     * For every observation value ({@link #names()} order) the divisor that produced it, so tooling can talk in real
     * units: value = real / scale. The V2 block is normalised by {@code AiConstants.NORMALIZATION_VECTOR}; the appended
     * blocks scale themselves (a 1.0 here means "the block already writes its own units", see {@link #LAYOUT}).
     */
    /**
     * Which kept columns come from the private hand encoder. A position of the OPPONENT can only be judged from public
     * information, so these are zeroed by {@link #buildPublic(MarsGame, Player)} - otherwise the one-ply search's
     * symmetric reading of the position would look into the opponent's hand (found 2026-11-15).
     */
    private static final boolean[] FROM_HAND = handMask();

    private static boolean[] handMask() {
        List<String> v2 = new ArrayList<>(CompleteTableEncoder.getAllFeatureNames());
        for (String name : CompleteHandEncoder.getAllFeatureNames()) {
            v2.add("hand:" + name);
        }
        boolean[] mask = new boolean[KEEP.length];
        for (int i = 0; i < KEEP.length; i++) {
            mask[i] = v2.get(KEEP[i]).startsWith("hand:");
        }
        return mask;
    }

    /** Every card class that owns an observation bit: the audit checks that each one can actually be played here. */
    public static List<Class<? extends Card>> auditedCardBits() {
        List<Class<? extends Card>> cards = new ArrayList<>();
        cards.addAll(TABLE_CARDS);
        cards.addAll(SCIENCE_CARDS);
        cards.addAll(HAND_CARDS);
        return cards;
    }

    public static float[] scales() {
        float[] scales = new float[size()];
        int pos = 0;
        for (int column : KEEP) {
            scales[pos++] = com.terraforming.ares.services.ai.AiConstants.NORMALIZATION_VECTOR[column];
        }
        // ctx, cards, extras, double payment, phase-start rules, special design, oceans
        while (pos < scales.length) {
            scales[pos++] = 1f;
        }
        return scales;
    }

    public static List<String> names() {
        List<String> v2 = new ArrayList<>(CompleteTableEncoder.getAllFeatureNames());
        for (String name : CompleteHandEncoder.getAllFeatureNames()) {
            v2.add("hand:" + name);
        }
        List<String> names = new ArrayList<>();
        for (int column : KEEP) {
            names.add(v2.get(column));
        }
        names.add("ctx:picks_are_known");
        for (int phase = 1; phase <= PHASES; phase++) names.add("ctx:current_phase_" + phase);
        names.add("ctx:current_phase_sell");
        for (String who : List.of("my", "opponent", "dummy")) {
            for (int phase = 1; phase <= PHASES; phase++) names.add("ctx:" + who + "_pick_" + phase);
        }
        for (String who : List.of("my", "opponent")) {
            names.add("ctx:" + who + "_green_builds");
            names.add("ctx:" + who + "_blue_red_builds");
        }
        for (String what : List.of("extra_blue_activations", "luna_project_office", "project_inspection", "activated_blue_cards")) {
            names.add("ctx:my_" + what);
            names.add("ctx:opponent_" + what);
        }
        for (String who : List.of("my", "opponent")) {
            for (Class<? extends Card> card : TABLE_CARDS) names.add("cards:" + who + "_on_table_" + card.getSimpleName());
        }
        for (String who : List.of("my", "opponent")) {
            for (Class<? extends Card> card : SCIENCE_CARDS) {
                names.add("cards:" + who + "_science_on_" + card.getSimpleName() + "/10");
            }
        }
        names.add("score:my_winning_points");
        names.add("score:opponent_winning_points");
        for (Class<? extends Card> card : HAND_CARDS) names.add("cards:in_my_hand_" + card.getSimpleName());
        names.add("extra:round/40");
        for (int phase = 1; phase <= PHASES; phase++) names.add("extra:my_previous_phase_" + phase);
        for (int phase = 1; phase <= PHASES; phase++) names.add("extra:opponent_previous_phase_" + phase);
        for (int phase = 1; phase <= PHASES; phase++) names.add("extra:dummy_pool_has_phase_" + phase);
        names.add("extra:dummy_pool_size/5");
        for (String who : List.of("my", "opponent")) {
            for (String dimension : DOUBLE_PAYMENT_DIMENSIONS) {
                names.add("cards:" + who + "_max_double_payment_" + dimension + "/10");
            }
        }
        for (String global : PHASE_START_DIMENSIONS) {
            names.add("planet:open_at_phase_start_" + global);
        }
        for (String who : List.of("my", "opponent")) {
            names.add("player:" + who + "_special_design_last_turn");
        }
        for (String dimension : OCEAN_DIMENSIONS) {
            names.add("oceans:last_opened_" + dimension + "/10");
        }
        for (String dimension : OCEAN_DIMENSIONS) {
            names.add("oceans:collected_" + dimension + "/40");
        }
        names.add("oceans:revealed/9");
        return names;
    }

    public float[] build(MarsGame game, Player player) {
        return build(game, player, true);
    }

    /**
     * The same vector with the player's private information (the hand block) zeroed. The engine uses it to judge the
     * OPPONENT's position: their hand is hidden, so a value read from a full observation of the opponent would be a peek.
     */
    public float[] buildPublic(MarsGame game, Player player) {
        return build(game, player, false);
    }

    private float[] build(MarsGame game, Player player, boolean includeHand) {
        long started = System.nanoTime();
        Player opponent = game.getPlayerUuidToPlayer().values().stream().filter(p -> !p.getUuid().equals(player.getUuid())).findFirst().orElseThrow();

        float[] v2 = baseEncoder.collectData(game, player.getUuid());
        float[] observation = new float[size()];
        int pos = 0;
        for (int column : KEEP) {
            observation[pos++] = v2[column];
        }

        // ---- round context
        boolean known = PICKS_ARE_KNOWN.contains(game.getStateType());
        observation[pos++] = known ? 1f : 0f;
        int current = game.getCurrentPhase();
        for (int phase = 1; phase <= PHASES; phase++) {
            observation[pos++] = known && current == phase ? 1f : 0f;
        }
        observation[pos++] = known && current == 6 ? 1f : 0f;
        pos = oneHot(observation, pos, known ? player.getChosenPhase() : null);
        pos = oneHot(observation, pos, known ? opponent.getChosenPhase() : null);
        Integer dummy = null;
        if (known && game.isDummyHandMode() && game.getUsedDummyHand() != null && !game.getUsedDummyHand().isEmpty()) {
            dummy = game.getCurrentDummyHand();
        }
        pos = oneHot(observation, pos, dummy);
        pos = builds(observation, pos, player);
        pos = builds(observation, pos, opponent);
        observation[pos++] = player.getBlueActionExtraActivationsLeft() / 2f;
        observation[pos++] = opponent.getBlueActionExtraActivationsLeft() / 2f;
        observation[pos++] = player.getLunaProjectOffice();
        observation[pos++] = opponent.getLunaProjectOffice();
        observation[pos++] = player.getProjectInspection();
        observation[pos++] = opponent.getProjectInspection();
        observation[pos++] = (player.getActivatedBlueCards().size() + player.getActivatedBlueCardsTwice().size()) / 10f;
        observation[pos++] = (opponent.getActivatedBlueCards().size() + opponent.getActivatedBlueCardsTwice().size()) / 10f;

        // ---- cards the V2 encoder does not tell apart
        pos = onTable(observation, pos, player);
        pos = onTable(observation, pos, opponent);
        pos = scienceCounters(observation, pos, player);
        pos = scienceCounters(observation, pos, opponent);
        // The score, one number per player, from the engine's own total.
        observation[pos++] = winPointsService.countWinPoints(player, game);
        observation[pos++] = winPointsService.countWinPoints(opponent, game);
        for (Class<? extends Card> card : HAND_CARDS) {
            observation[pos++] = player.getHand().getCards().stream().anyMatch(id -> card.isInstance(cardService.getCard(id))) ? 1f : 0f;
        }

        // ---- extras: history and the dummy pool
        observation[pos++] = game.getTurns() / 40f;
        pos = oneHot(observation, pos, player.getPreviousChosenPhase());
        pos = oneHot(observation, pos, opponent.getPreviousChosenPhase());
        Set<Integer> pool = PickPhaseDecisions.dummyPhasePool(game);
        for (int phase = 1; phase <= PHASES; phase++) {
            observation[pos++] = pool.contains(phase) ? 1f : 0f;
        }
        observation[pos++] = pool.isEmpty() ? 0f : pool.size() / (float) PHASES;
        pos = doublePayment(observation, pos, game, player);
        pos = doublePayment(observation, pos, game, opponent);
        // One bit per global: "this could still be advanced when the phase started" (the engine's own rule).
        Planet start = game.getPlanetAtTheStartOfThePhase();
        observation[pos++] = start == null || !start.isTemperatureMax() ? 1f : 0f;
        observation[pos++] = start == null || !start.isOxygenMax() ? 1f : 0f;
        observation[pos++] = start == null || !start.allOceansRevealed() ? 1f : 0f;
        // Rule predicate: a card with a "special design" built last turn may ignore global requirements this turn.
        observation[pos++] = player.isBuiltSpecialDesignLastTurn() ? 1f : 0f;
        observation[pos++] = opponent.isBuiltSpecialDesignLastTurn() ? 1f : 0f;
        // Oceans: the reward of the last opened one (what every further purchase pays once all nine are open), the totals
        // collected so far and how many are revealed. Positions are a shuffle and are not encoded.
        Planet planet = game.getPlanet();
        int revealed = 0;
        int cards = 0;
        int mc = 0;
        int plants = 0;
        for (Ocean ocean : planet.getOceans()) {
            if (ocean.isRevealed()) {
                revealed++;
                cards += ocean.getCards();
                mc += ocean.getMc();
                plants += ocean.getPlants();
            }
        }
        // NOT "the last one in the list": the engine pays the ocean it recorded in `lastOpenedOceanIndex`, and every
        // further purchase after the ninth is exactly that one (`Planet.revealOcean`).
        int lastIndex = planet.getLastOpenedOceanIndex();
        Ocean last = lastIndex >= 0 && lastIndex < planet.getOceans().size() ? planet.getOceans().get(lastIndex) : null;
        int lastCards = last == null ? 0 : last.getCards();
        int lastMc = last == null ? 0 : last.getMc();
        int lastPlants = last == null ? 0 : last.getPlants();
        observation[pos++] = lastCards / 10f;
        observation[pos++] = lastMc / 10f;
        observation[pos++] = lastPlants / 10f;
        observation[pos++] = cards / 40f;
        observation[pos++] = mc / 40f;
        observation[pos++] = plants / 40f;
        observation[pos++] = revealed / 9f;
        if (pos != observation.length) {
            throw new IllegalStateException("The observation layout is inconsistent: wrote " + pos + " of " + observation.length);
        }
        if (!includeHand) {
            for (int i = 0; i < FROM_HAND.length; i++) {
                if (FROM_HAND[i]) {
                    observation[i] = 0f;
                }
            }
        }
        AgentPerfStats.recordObservation(System.nanoTime() - started);
        return observation;
    }

    /**
     * The best single double payment this player can get in phase 4, per dimension: for every played card that can pay
     * again, the card's {@code payAgain} is applied to a copy of the position and the change of every counter is
     * measured; the maximum per dimension is written. The number of such cards is a handful, so this costs a few
     * microseconds and needs no static table of 130 cards - and cards whose payment depends on the state (steel, science,
     * tags) are handled by construction.
     */
    private int doublePayment(float[] target, int pos, MarsGame game, Player player) {
        int[] best = new int[DOUBLE_PAYMENT_DIMENSIONS.size()];
        // ONE FRESH COPY OF THE GAME PER CARD. The phase-4 upgrade makes exactly one played card pay again, on the true
        // position, so each card has to be measured on its own copy: with a shared copy a card's payment can depend on
        // what another card's payAgain changed in the copy (a deck draw, a global), and the feature then reports a payment
        // that cannot happen in the game (caught by ObservationCompletenessTest, 2026-11-15). Cheap: a handful of played
        // cards can pay again at all.
        for (Integer id : player.getPlayed().getCards()) {
            Card card = cardService.getCard(id);
            if (card == null || !card.canPayAgain()) {
                continue;
            }
            MarsGame host = new MarsGame(game);
            Player probe = new Player(player);
            try {
                card.payAgain(host, cardService, probe);
            } catch (RuntimeException e) {
                continue; // a card that cannot pay in this state simply contributes nothing
            }
            int[] delta = {
                    probe.getMc() - player.getMc(),
                    probe.getPlants() - player.getPlants(),
                    probe.getHeat() - player.getHeat(),
                    probe.getHand().size() - player.getHand().size(),
                    probe.getTerraformingRating() - player.getTerraformingRating(),
                    probe.getMcIncome() - player.getMcIncome(),
                    probe.getPlantsIncome() - player.getPlantsIncome(),
                    probe.getHeatIncome() - player.getHeatIncome(),
                    probe.getCardIncome() - player.getCardIncome(),
                    probe.getSteelIncome() - player.getSteelIncome(),
                    probe.getTitaniumIncome() - player.getTitaniumIncome()
            };
            for (int i = 0; i < best.length; i++) {
                if (delta[i] > best[i]) {
                    best[i] = delta[i];
                }
            }
        }
        for (int value : best) {
            target[pos++] = value / 10f;
        }
        return pos;
    }

    private int onTable(float[] target, int pos, Player player) {
        for (Class<? extends Card> card : TABLE_CARDS) {
            target[pos++] = player.getPlayed().getCards().stream().anyMatch(id -> card.isInstance(cardService.getCard(id))) ? 1f : 0f;
        }
        return pos;
    }

    /** Science on our own cards, the same shape the animal/microbe counters have (a count per card, /10). */
    private int scienceCounters(float[] target, int pos, Player player) {
        for (Class<? extends Card> card : SCIENCE_CARDS) {
            Integer count = player.getCardResourcesCount().get(card);
            target[pos++] = (count == null ? 0 : count) / 10f;
        }
        return pos;
    }

    /**
     * Build slots still available, per colour. The slot's discount and price limit are NOT encoded: both come from the
     * phase-upgrade cards (already one-hot in the V2 block) and from which phase is being played, so they are derived
     * information, and a net that sees the hand and the money does not need them spelled out.
     */
    private static int builds(float[] target, int pos, Player player) {
        int green = 0;
        int blueRed = 0;
        for (BuildDto build : player.getBuilds()) {
            green += build.getType().isGreen() ? 1 : 0;
            blueRed += build.getType().isBlueRed() ? 1 : 0;
        }
        target[pos++] = green;
        target[pos++] = blueRed;
        return pos;
    }

    private static int oneHot(float[] target, int pos, Integer phase) {
        if (phase != null && phase >= 1 && phase <= PHASES) {
            target[pos + phase - 1] = 1f;
        }
        return pos + PHASES;
    }
}
