package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.SpecialEffectsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decisions that name a set of cards one card at a time.
 * <p>
 * A draft ({@link DecisionType#DRAFT_CARDS}) or a forced discard ({@link DecisionType#DISCARD_CARDS}) drops exactly
 * the number the engine asked for, so there is no stop candidate. A sale is optional beyond its minimum, so the last
 * candidate is stop: take none of the remaining cards.
 * <p>
 * Selling during a phase ({@link DecisionType#SELL_CARDS}) is that kind of choice. The engine lists it next to building
 * and the blue-action menu, and the turn is not terminal, so the money arrives and the build or the action is still
 * there. The stop candidate on the first step is "do the other action instead". The payout is the same for every card
 * (3, plus one per sell-price effect) and is written on the candidate so the effect is visible without a new
 * observation slot.
 */
@Service
@RequiredArgsConstructor
public class HandCardDecisions {
    /** Largest set offered at one step. A phase-5 reveal stays far under this; a bigger pool has to be a conscious change. */
    public static final int POOL = 40;
    public static final int MAX_CANDIDATES = POOL + 1;
    public static final int SELL_FEATURES = CardFeatures.LENGTH + 1;

    private static final String CARD = "classSignature(" + CardFeatures.SIGNATURE + "), color(" + CardFeatures.COLORS
            + "), tags(" + CardFeatures.TAGS + "), price/50, active, stop";

    public static final String DRAFT_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[" + CARD + "]; discard exactly the required number from the revealed cards; no stop; stopId="
            + CardFeatures.STOP_ID;
    public static final String DISCARD_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[" + CARD + "]; discard exactly the required number from the hand; no stop; stopId="
            + CardFeatures.STOP_ID;
    public static final String SELL_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[" + CARD + ", sellMc/10]; stop keeps the rest and takes the other action; stopId="
            + CardFeatures.STOP_ID;
    public static final String SELL_LAST_ROUND_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[" + CARD + ", sellMc/10]; sell down to the hand limit, stop once that is reached; stopId="
            + CardFeatures.STOP_ID;

    private final ObservationBuilder observationBuilder;
    private final CardService cardService;
    private final SpecialEffectsService specialEffectsService;

    public static DecisionFile.Schema schema(DecisionType type) {
        return new DecisionFile.Schema(type, ObservationBuilder.size(), type.maxCandidates(), width(type), type.observationLayout());
    }

    public static int width(DecisionType type) {
        return switch (type) {
            case DRAFT_CARDS, DISCARD_CARDS -> CardFeatures.LENGTH;
            case SELL_CARDS, SELL_CARDS_LAST_ROUND -> SELL_FEATURES;
            default -> throw new IllegalArgumentException(type + " is not a card-set decision");
        };
    }

    /**
     * @param pool     cards the engine allows (the revealed set, or the hand)
     * @param already  cards chosen on the earlier steps of this same set
     * @param mayStop  stop is legal; false while a draft or a discard still owes cards, and while a last-round sale
     *                 has not reached the hand limit yet
     */
    public Decision build(DecisionType type, MarsGame game, Player player, List<Integer> pool,
                          List<Integer> already, boolean mayStop) {
        Map<Integer, Integer> left = new HashMap<>();
        for (int id : pool) {
            left.merge(id, 1, Integer::sum);
        }
        for (int id : already) {
            Integer copies = left.get(id);
            if (copies == null) {
                throw new IllegalArgumentException(type + " picks " + id + " which is no longer in the pool");
            }
            if (copies == 1) {
                left.remove(id);
            } else {
                left.put(id, copies - 1);
            }
        }

        boolean sale = type == DecisionType.SELL_CARDS || type == DecisionType.SELL_CARDS_LAST_ROUND;
        float payout = sale ? specialEffectsService.getCardPrice(player) / 10f : 0f;
        List<Integer> ids = new ArrayList<>(left.keySet());
        ids.sort(Integer::compareTo);
        List<Candidate> candidates = new ArrayList<>();
        for (int id : ids) {
            Card card = cardService.getCard(id);
            for (int copy = 0; copy < left.get(id); copy++) {
                candidates.add(new Candidate(id, features(card, false, sale, payout)));
            }
        }
        if (mayStop) {
            candidates.add(new Candidate(CardFeatures.STOP_ID, features(null, true, sale, 0f)));
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException(type + " has nothing to choose and cannot stop");
        }
        if (candidates.size() > MAX_CANDIDATES) {
            throw new IllegalStateException(type + " offers " + candidates.size()
                    + " candidates; the decision is sized for " + MAX_CANDIDATES);
        }
        return new Decision(type, player.getUuid(), observationBuilder.build(game, player), candidates);
    }

    static float[] features(Card card, boolean stop, boolean sale, float payout) {
        float[] base = CardFeatures.of(card, stop);
        if (!sale) {
            return base;
        }
        float[] features = new float[SELL_FEATURES];
        System.arraycopy(base, 0, features, 0, base.length);
        if (!stop) {
            features[base.length] = payout;
        }
        return features;
    }
}
