package com.terraforming.ares.services.agent;

import com.terraforming.ares.cards.blue.AnaerobicMicroorganisms;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.payments.AnaerobicMicroorganismsPayment;
import com.terraforming.ares.model.payments.HeatPayment;
import com.terraforming.ares.model.payments.MegacreditsPayment;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.payments.RestructuredResourcesPayment;
import com.terraforming.ares.services.BuildService;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.DiscountService;
import com.terraforming.ares.services.SpecialEffectsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * How to pay for a card that was already chosen ({@link DecisionType#BUILD_CARD}). Candidates are every combination of
 * Restructured Resources and Anaerobic Microorganisms that the player can afford, with the remaining price covered by
 * MC (and heat for Helion). Each candidate is checked with {@link CardValidationService#validateCard}.
 * <p>
 * Helion may split the remaining price between MC and heat; every split is a separate candidate. Worst case stays under
 * {@link #MAX_CANDIDATES} for ordinary card prices.
 */
@Service
@RequiredArgsConstructor
public class BuildPaymentDecisions {
    public static final int MAX_CANDIDATES = 200;
    public static final int CANDIDATE_FEATURES = 6;
    public static final String OBSERVATION_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[restructured, anaerobic, heat/20, mc/20, need/20, discounts/20]; "
            + "id=(heat<<2)|(anaerobic?2:0)|(restructured?1:0)";

    private final ObservationBuilder observationBuilder;
    private final DiscountService discountService;
    private final BuildService buildService;
    private final SpecialEffectsService specialEffectsService;
    private final CardService cardService;
    private final CardValidationService cardValidationService;

    public static DecisionFile.Schema schema() {
        return new DecisionFile.Schema(DecisionType.BUILD_PAYMENT, ObservationBuilder.size(),
                DecisionType.BUILD_PAYMENT.maxCandidates(), CANDIDATE_FEATURES, OBSERVATION_LAYOUT);
    }

    public static int packId(boolean restructured, boolean anaerobic, int heat) {
        return (heat << 2) | (anaerobic ? 2 : 0) | (restructured ? 1 : 0);
    }

    public List<BuildPaymentChoice> options(MarsGame game, Player player, Card card,
                                            Map<Integer, List<Integer>> inputParams) {
        int free = discountService.getDiscount(game, card, player, inputParams);
        boolean canRestructured = canUseRestructured(player, card);
        boolean canAnaerobic = player.getCardResourcesCount().getOrDefault(AnaerobicMicroorganisms.class, 0) >= 2;
        boolean helion = isHelion(player);
        int heatAvailable = helion ? Math.max(0, player.getHeat() - card.heatSpendOnBuild()) : 0;

        List<BuildPaymentChoice> choices = new ArrayList<>();
        for (boolean restructured : new boolean[]{false, true}) {
            if (restructured && !canRestructured) {
                continue;
            }
            for (boolean anaerobic : new boolean[]{false, true}) {
                if (anaerobic && !canAnaerobic) {
                    continue;
                }
                int paidDiscount = (restructured ? 5 : 0) + (anaerobic ? 10 : 0);
                int beforeBuild = free + paidDiscount;
                BuildDto build = buildService.findMostOptimalBuild(card, player, beforeBuild);
                if (build == null) {
                    continue;
                }
                int need = Math.max(0, card.getPrice() - beforeBuild - build.getExtraDiscount());
                int maxHeat = Math.min(heatAvailable, need);
                for (int heat = 0; heat <= maxHeat; heat++) {
                    int mc = need - heat;
                    if (mc > player.getMc()) {
                        continue;
                    }
                    List<Payment> payments = new ArrayList<>();
                    if (restructured) {
                        payments.add(new RestructuredResourcesPayment());
                    }
                    if (anaerobic) {
                        payments.add(new AnaerobicMicroorganismsPayment());
                    }
                    if (mc > 0 || heat == 0) {
                        payments.add(new MegacreditsPayment(mc));
                    }
                    if (heat > 0) {
                        payments.add(new HeatPayment(heat));
                    }
                    if (cardValidationService.validateCard(player, game, card.getId(), payments, inputParams) != null) {
                        continue;
                    }
                    choices.add(new BuildPaymentChoice(packId(restructured, anaerobic, heat), List.copyOf(payments)));
                }
            }
        }
        if (choices.isEmpty()) {
            throw new IllegalStateException("No legal payment for card " + card.getId());
        }
        if (choices.size() > MAX_CANDIDATES) {
            throw new IllegalStateException("Payment menu has " + choices.size() + " options for card " + card.getId());
        }
        return choices;
    }

    public Decision decision(MarsGame game, Player player, Card card, List<BuildPaymentChoice> choices) {
        List<Candidate> candidates = new ArrayList<>(choices.size());
        for (BuildPaymentChoice choice : choices) {
            candidates.add(new Candidate(choice.id(), features(choice)));
        }
        return new Decision(DecisionType.BUILD_PAYMENT, player.getUuid(),
                observationBuilder.build(game, player), candidates);
    }

    static float[] features(BuildPaymentChoice choice) {
        float[] features = new float[CANDIDATE_FEATURES];
        int heat = choice.id() >>> 2;
        boolean anaerobic = (choice.id() & 2) != 0;
        boolean restructured = (choice.id() & 1) != 0;
        int mc = 0;
        for (Payment payment : choice.payments()) {
            if (payment instanceof MegacreditsPayment) {
                mc = payment.getValue();
            }
        }
        int discounts = (restructured ? 5 : 0) + (anaerobic ? 10 : 0);
        features[0] = restructured ? 1f : 0f;
        features[1] = anaerobic ? 1f : 0f;
        features[2] = heat / 20f;
        features[3] = mc / 20f;
        features[4] = (mc + heat) / 20f;
        features[5] = discounts / 20f;
        return features;
    }

    private boolean canUseRestructured(Player player, Card card) {
        if (!specialEffectsService.ownsSpecialEffect(player, SpecialEffect.RESTRUCTURED_RESOURCES)) {
            return false;
        }
        int plantCost = 1;
        if (card.getCardMetadata() != null) {
            plantCost += card.getCardMetadata().getBonuses().stream()
                    .filter(gain -> gain.getType() == GainType.PLANT && gain.getValue() < 0)
                    .mapToInt(gain -> -gain.getValue())
                    .sum();
        }
        return player.getPlants() >= plantCost;
    }

    private boolean isHelion(Player player) {
        if (player.getSelectedCorporationCard() == null) {
            return false;
        }
        Card corporation = cardService.getCard(player.getSelectedCorporationCard());
        return corporation.getCardMetadata() != null
                && corporation.getCardMetadata().getCardAction() == CardAction.HELION_CORPORATION;
    }
}
