package com.terraforming.ares.services.ai.helpers;

import com.terraforming.ares.cards.blue.AnaerobicMicroorganisms;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.payments.AnaerobicMicroorganismsPayment;
import com.terraforming.ares.model.payments.MegacreditsPayment;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.payments.RestructuredResourcesPayment;
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
public class AiPaymentService {
    private final SpecialEffectsService specialEffectsService;
    private final DiscountService discountService;
    private final BuildService buildService;

    public List<Payment> getCardPayments(MarsGame game, Player player, Card card, Map<Integer, List<Integer>> inputParameters) {
        int discount = discountService.getDiscount(game, card, player, inputParameters);

        int price = Math.max(0, card.getPrice() - discount);

        boolean applyRestructuredResources = false;
        boolean applyAnaerobicMicroorganisms = false;

        if (!card.getTags().contains(Tag.DYNAMIC)) {//TODO consider dynamic with discount

            if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.RESTRUCTURED_RESOURCES)) {
                if (price >= 3 && player.getPlants() > 0) {
                    applyRestructuredResources = true;
                }
            }

            if (applyRestructuredResources) {
                price -= 5;
            }

            if (player.getCardResourcesCount().getOrDefault(AnaerobicMicroorganisms.class, 0) >= 2) {
                if (price >= 9) {
                    applyAnaerobicMicroorganisms = true;
                }
            }

            if (applyAnaerobicMicroorganisms) {
                price -= 10;
            }
        }

        price = Math.max(0, price);

        int finalDiscount = card.getPrice() - price;
        final BuildDto mostOptimalBuild = buildService.findMostOptimalBuild(card, player, finalDiscount);

        if (mostOptimalBuild != null) {
            price = Math.max(0, price - mostOptimalBuild.getExtraDiscount());
        }

        List<Payment> payments = new ArrayList<>();
        payments.add(new MegacreditsPayment(price));
        if (applyRestructuredResources) {
            payments.add(new RestructuredResourcesPayment());
        }
        if (applyAnaerobicMicroorganisms) {
            payments.add(new AnaerobicMicroorganismsPayment());
        }

        return payments;
    }

}
