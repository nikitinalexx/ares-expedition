package com.terraforming.ares.services.ai.turnProcessors.random;

import com.terraforming.ares.cards.blue.AnaerobicMicroorganisms;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.payments.*;
import com.terraforming.ares.services.BuildService;
import com.terraforming.ares.services.DiscountService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.dto.PaidDiscount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.terraforming.ares.services.ai.dto.PaidDiscount.ANAEROBIC_DISCOUNT;
import static com.terraforming.ares.services.ai.dto.PaidDiscount.RESTRUCTURED_DISCOUNT;

/**
 * Created by oleksii.nikitin
 * Creation date 24.11.2022
 */
@Service
@RequiredArgsConstructor
public class AiRandomPaymentService {
    private final SpecialEffectsService specialEffectsService;
    private final DiscountService discountService;
    private final BuildService buildService;

    public List<Payment> getCardPayments(MarsGame game, Player player, Card card, Map<Integer, List<Integer>> inputParameters) {
        List<Payment> payments = new ArrayList<>();

        /* -------------------------------------------------
         * 1. FREE DISCOUNTS (теги, доходы, корпорации)
         * ------------------------------------------------- */
        int freeDiscount = discountService.getDiscount(game, card, player, inputParameters);
        int price = Math.max(0, card.getPrice() - freeDiscount);

        /* -------------------------------------------------
         * 2. BUILD-CONTEXT DISCOUNT (SRB, next-card, etc.)
         * ------------------------------------------------- */
        int discountAlreadyApplied = card.getPrice() - price;
        BuildDto optimalBuild = buildService.findMostOptimalBuild(card, player, discountAlreadyApplied);

        if (optimalBuild != null && price > 0) {
            int buildDiscount = Math.min(price, optimalBuild.getExtraDiscount());
            price -= buildDiscount;
        }

        /* -------------------------------------------------
         * 3. PAID DISCOUNTS (ресурсы)
         * ------------------------------------------------- */
        List<PaidDiscount> paidDiscounts = new ArrayList<>();

        // Restructured Resources: -5 за 1 растение
        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.RESTRUCTURED_RESOURCES) && player.getPlants() > 0) {
            int requiredPlants = 1; // цена скидки

            if (card.getCardMetadata() != null) {
                for (Gain gain : card.getCardMetadata().getBonuses()) {
                    if (gain.getType() == GainType.PLANT && gain.getValue() < 0) {
                        requiredPlants -= gain.getValue(); // минус минус = плюс
                    }
                }
            }

            if (player.getPlants() >= requiredPlants) {
                paidDiscounts.add(RESTRUCTURED_DISCOUNT);
            }
        }

        // Anaerobic Microorganisms: -10 за 2 микроба
        if (player.getCardResourcesCount().getOrDefault(AnaerobicMicroorganisms.class, 0) >= 2) {
            paidDiscounts.add(ANAEROBIC_DISCOUNT);
        }

        // сортируем по реальному эффекту на текущую цену
        int finalPrice = price;
        paidDiscounts.sort((a, b) -> {
            int aReal = Math.min(finalPrice, a.getValue());
            int bReal = Math.min(finalPrice, b.getValue());
            return Integer.compare(bReal, aReal);
        });

        for (PaidDiscount discount : paidDiscounts) {
            if (price <= 0) break;

            int realDiscount = Math.min(price, discount.getValue());
            if (realDiscount <= 0) continue;

            price -= realDiscount;

            if (discount.getApplyPayment() == PaymentType.ANAEROBIC_MICROORGANISMS) {
                payments.add(new AnaerobicMicroorganismsPayment());
            } else if (discount.getApplyPayment() == PaymentType.RESTRUCTURED_RESOURCES) {
                payments.add(new RestructuredResourcesPayment());
            }
        }

        /* -------------------------------------------------
         * 4. FINAL MC PAYMENT
         * ------------------------------------------------- */
        payments.add(new MegacreditsPayment(Math.max(0, price)));

        return payments;
    }


}
