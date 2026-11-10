package com.terraforming.ares.services.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.cards.blue.FilterFeeders;
import com.terraforming.ares.cards.blue.MatterGenerator;
import com.terraforming.ares.cards.blue.Pets;
import com.terraforming.ares.cards.blue.Sawmill;
import com.terraforming.ares.cards.green.ArchitectureBlueprints;
import com.terraforming.ares.cards.green.BedrockWellbore;
import com.terraforming.ares.cards.green.CityPlanning;
import com.terraforming.ares.cards.green.GrainSilos;
import com.terraforming.ares.cards.green.InvasiveIrrigation;
import com.terraforming.ares.services.CardService;
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
    public static final int CONTEXT_SIZE = 1 + 5 + 1 + 15 + 2 * 4 + 2 + 2 + 2 + 2;
    public static final int EXTRAS_SIZE = 1 + PHASES + PHASES + PHASES + 1;

    /**
     * Cards the V2 encoder can not tell apart (found by CardTrackingAuditTest; the V2 encoder must not be changed - NETWORK_V2 is trained on it):
     * blue cards with an action or a resource that matter when they are on the table, and newest cards that the hand encoder does not know.
     */
    static final List<Class<? extends Card>> TABLE_CARDS = List.of(MatterGenerator.class, Sawmill.class, FilterFeeders.class, Pets.class);
    static final List<Class<? extends Card>> RESOURCE_CARDS = List.of(FilterFeeders.class, Pets.class);
    static final List<Class<? extends Card>> HAND_CARDS = List.of(BedrockWellbore.class, ArchitectureBlueprints.class, CityPlanning.class,
            GrainSilos.class, InvasiveIrrigation.class);
    public static final int CARDS_SIZE = 2 * TABLE_CARDS.size() + 2 * RESOURCE_CARDS.size() + HAND_CARDS.size();

    public static final String LAYOUT = "ObservationBuilder v3.1: V2 encoder (table 417 + hand 481) without 210 exact copies (observation/v3_mask.json)"
            + " + roundContext[revealed, currentPhase(5), sellFlag, picks me/opponent/dummy (15), builds me/opponent (8), extra blue activations (2),"
            + " luna office (2), project inspection (2), activated blue cards (2)]"
            + " + cards[on table me/opponent: MatterGenerator Sawmill FilterFeeders Pets (8), resources on FilterFeeders Pets me/opponent (4),"
            + " in my hand: BedrockWellbore ArchitectureBlueprints CityPlanning GrainSilos InvasiveIrrigation (5)]"
            + " + extras[round/40, myPreviousPhase(5), opponentPreviousPhase(5), dummyPoolMembership(5), dummyPoolSize/5]";

    private static final Set<StateType> PICKS_ARE_KNOWN = Set.of(StateType.BUILD_GREEN_PROJECTS, StateType.BUILD_BLUE_RED_PROJECTS,
            StateType.PERFORM_BLUE_ACTION, StateType.COLLECT_INCOME, StateType.DRAFT_CARDS, StateType.SELL_EXTRA_CARDS);

    private static final int[] KEEP = loadMask();

    private final AdvancedAiDataCollectionService baseEncoder;
    private final CardService cardService;

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
    public static int[] keptColumns() {
        return KEEP.clone();
    }

    public static int size() {
        return KEEP.length + CONTEXT_SIZE + CARDS_SIZE + EXTRAS_SIZE;
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
            names.add("ctx:" + who + "_best_build_discount/20");
            names.add("ctx:" + who + "_tightest_build_price_limit/30");
        }
        for (String what : List.of("extra_blue_activations", "luna_project_office", "project_inspection", "activated_blue_cards")) {
            names.add("ctx:my_" + what);
            names.add("ctx:opponent_" + what);
        }
        for (String who : List.of("my", "opponent")) {
            for (Class<? extends Card> card : TABLE_CARDS) names.add("cards:" + who + "_on_table_" + card.getSimpleName());
        }
        for (String who : List.of("my", "opponent")) {
            for (Class<? extends Card> card : RESOURCE_CARDS) names.add("cards:" + who + "_resources_on_" + card.getSimpleName() + "/10");
        }
        for (Class<? extends Card> card : HAND_CARDS) names.add("cards:in_my_hand_" + card.getSimpleName());
        names.add("extra:round/40");
        for (int phase = 1; phase <= PHASES; phase++) names.add("extra:my_previous_phase_" + phase);
        for (int phase = 1; phase <= PHASES; phase++) names.add("extra:opponent_previous_phase_" + phase);
        for (int phase = 1; phase <= PHASES; phase++) names.add("extra:dummy_pool_has_phase_" + phase);
        names.add("extra:dummy_pool_size/5");
        return names;
    }

    public float[] build(MarsGame game, Player player) {
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
        pos = resources(observation, pos, player);
        pos = resources(observation, pos, opponent);
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
        if (pos != observation.length) {
            throw new IllegalStateException("The observation layout is inconsistent: wrote " + pos + " of " + observation.length);
        }
        return observation;
    }

    private int onTable(float[] target, int pos, Player player) {
        for (Class<? extends Card> card : TABLE_CARDS) {
            target[pos++] = player.getPlayed().getCards().stream().anyMatch(id -> card.isInstance(cardService.getCard(id))) ? 1f : 0f;
        }
        return pos;
    }

    private static int resources(float[] target, int pos, Player player) {
        for (Class<? extends Card> card : RESOURCE_CARDS) {
            target[pos++] = player.getCardResourcesCount().getOrDefault(card, 0) / 10f;
        }
        return pos;
    }

    private static int builds(float[] target, int pos, Player player) {
        int green = 0;
        int blueRed = 0;
        int bestDiscount = 0;
        int tightestLimit = 0;
        for (BuildDto build : player.getBuilds()) {
            green += build.getType().isGreen() ? 1 : 0;
            blueRed += build.getType().isBlueRed() ? 1 : 0;
            bestDiscount = Math.max(bestDiscount, build.getExtraDiscount());
            if (build.getPriceLimit() > 0) {
                tightestLimit = tightestLimit == 0 ? build.getPriceLimit() : Math.min(tightestLimit, build.getPriceLimit());
            }
        }
        target[pos++] = green;
        target[pos++] = blueRed;
        target[pos++] = bestDiscount / 20f;
        target[pos++] = tightestLimit / 30f;
        return pos;
    }

    private static int oneHot(float[] target, int pos, Integer phase) {
        if (phase != null && phase >= 1 && phase <= PHASES) {
            target[pos + phase - 1] = 1f;
        }
        return pos + PHASES;
    }
}
