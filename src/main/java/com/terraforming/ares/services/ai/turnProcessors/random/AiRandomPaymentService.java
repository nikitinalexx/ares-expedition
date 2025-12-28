package com.terraforming.ares.services.ai.turnProcessors.random;

import com.terraforming.ares.cards.blue.AnaerobicMicroorganisms;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.payments.*;
import com.terraforming.ares.services.BuildService;
import com.terraforming.ares.services.DiscountService;
import com.terraforming.ares.services.SpecialEffectsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            paidDiscounts.add(RESTRUCTURED_DISCOUNT);
        }

        // Anaerobic Microorganisms: -10 за 2 микроба
        if (player.getCardResourcesCount().getOrDefault(AnaerobicMicroorganisms.class, 0) >= 2) {
            paidDiscounts.add(ANAEROBIC_DISCOUNT);
        }

        // сортируем по реальному эффекту на текущую цену
        int finalPrice = price;
        paidDiscounts.sort((a, b) -> {
            int aReal = Math.min(finalPrice, a.value);
            int bReal = Math.min(finalPrice, b.value);
            return Integer.compare(bReal, aReal);
        });

        for (PaidDiscount discount : paidDiscounts) {
            if (price <= 0) break;

            int realDiscount = Math.min(price, discount.value);
            if (realDiscount <= 0) continue;

            price -= realDiscount;

            if (discount.applyPayment == PaymentType.ANAEROBIC_MICROORGANISMS) {
                payments.add(new AnaerobicMicroorganismsPayment());
            } else if (discount.applyPayment == PaymentType.RESTRUCTURED_RESOURCES) {
                payments.add(new RestructuredResourcesPayment());
            }
        }

        /* -------------------------------------------------
         * 4. FINAL MC PAYMENT
         * ------------------------------------------------- */
        payments.add(new MegacreditsPayment(Math.max(0, price)));

        return payments;
    }

    /* =====================================================
     * PAID DISCOUNTS MODEL
     * ===================================================== */
    static final PaidDiscount ANAEROBIC_DISCOUNT =
            new PaidDiscount(10, PaymentType.ANAEROBIC_MICROORGANISMS);

    static final PaidDiscount RESTRUCTURED_DISCOUNT =
            new PaidDiscount(5, PaymentType.RESTRUCTURED_RESOURCES);

    static class PaidDiscount {
        final int value;
        final PaymentType applyPayment;

        PaidDiscount(int value, PaymentType applyPayment) {
            this.value = value;
            this.applyPayment = applyPayment;
        }
    }


}
