package com.terraforming.ares.services.ai.turnProcessors.network2;

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

@Service
@RequiredArgsConstructor
public class Network2PaymentService {
    private final DiscountService discountService;
    private final BuildService buildService;
    private final SpecialEffectsService specialEffectsService;

    public MegacreditsPayment getMcPaymentOnly(MarsGame game, Player player, Card card, Map<Integer, List<Integer>> inputParameters) {

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
         * 4. FINAL MC PAYMENT
         * ------------------------------------------------- */
        return new MegacreditsPayment(Math.max(0, price));
    }

    public List<List<Payment>> getAllPossibleCardPayments(MarsGame game, Player player, Card card, Map<Integer, List<Integer>> inputParameters) {
        List<List<Payment>> allVariations = new ArrayList<>();

        // 1. Считаем цену после всех бесплатных скидок
        int freeDiscount = discountService.getDiscount(game, card, player, inputParameters);
        int basePrice = Math.max(0, card.getPrice() - freeDiscount);

        // 2. Build-контекст
        int discountAlreadyApplied = card.getPrice() - basePrice;
        BuildDto optimalBuild = buildService.findMostOptimalBuild(card, player, discountAlreadyApplied);
        if (optimalBuild != null && basePrice > 0) {
            basePrice -= Math.min(basePrice, optimalBuild.getExtraDiscount());
        }

        // 3. Проверяем доступность платных ресурсов
        boolean canUsePlants = specialEffectsService.ownsSpecialEffect(player, SpecialEffect.RESTRUCTURED_RESOURCES)
                && player.getPlants() >= getRequiredPlants(card);

        boolean canUseMicrobes = player.getCardResourcesCount().getOrDefault(AnaerobicMicroorganisms.class, 0) >= 2;

        // 4. Генерируем варианты (всего 4 комбинации)

        // Вариант 1: Только деньги (всегда есть)
        allVariations.add(List.of(new MegacreditsPayment(basePrice)));

        // Вариант 2: Деньги + Растения (если есть смысл)
        if (canUsePlants && basePrice > 0) {
            int priceAfterPlants = Math.max(0, basePrice - RESTRUCTURED_DISCOUNT.getValue());
            allVariations.add(List.of(new RestructuredResourcesPayment(), new MegacreditsPayment(priceAfterPlants)));
        }

        // Вариант 3: Деньги + Микробы (если есть смысл)
        if (canUseMicrobes && basePrice > 0) {
            int priceAfterMicrobes = Math.max(0, basePrice - ANAEROBIC_DISCOUNT.getValue());
            allVariations.add(List.of(new AnaerobicMicroorganismsPayment(), new MegacreditsPayment(priceAfterMicrobes)));
        }

        // Вариант 4: Деньги + Растения + Микробы (если после первой скидки еще осталась цена)
        if (canUsePlants && canUseMicrobes && basePrice > 0) {
            int priceAfterBoth = Math.max(0, basePrice - RESTRUCTURED_DISCOUNT.getValue() - ANAEROBIC_DISCOUNT.getValue());

            // Проверяем, что вторая скидка не впустую.
            // Если уже после первой скидки цена стала 0, то этот вариант дублирует Вариант 2 или 3.
            int priceAfterFirst = Math.max(0, basePrice - RESTRUCTURED_DISCOUNT.getValue());
            if (priceAfterFirst > 0) {
                allVariations.add(List.of(
                        new RestructuredResourcesPayment(),
                        new AnaerobicMicroorganismsPayment(),
                        new MegacreditsPayment(priceAfterBoth)
                ));
            }
        }

        return allVariations;
    }

    private int getRequiredPlants(Card card) {
        int requiredPlants = 1; // цена скидки

        if (card.getCardMetadata() != null) {
            for (Gain gain : card.getCardMetadata().getBonuses()) {
                if (gain.getType() == GainType.PLANT && gain.getValue() < 0) {
                    requiredPlants -= gain.getValue(); // минус минус = плюс
                }
            }
        }

        return requiredPlants;
    }

}
