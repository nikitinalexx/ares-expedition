package com.terraforming.ares.validation.payment;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.payments.PaymentType;
import com.terraforming.ares.services.SpecialEffectsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
@Component
@RequiredArgsConstructor
public class RestructuredResourcesPaymentValidator implements PaymentValidator {
    private final SpecialEffectsService specialEffectsService;

    @Override
    public PaymentType getType() {
        return PaymentType.RESTRUCTURED_RESOURCES;
    }

    @Override
    public String validate(Player player, Payment payment) {
        if (!specialEffectsService.ownsSpecialEffect(player, SpecialEffect.RESTRUCTURED_RESOURCES)) {
            return "Type of payment not allowed " + PaymentType.RESTRUCTURED_RESOURCES;
        }

        if (player.getPlants() < 1) {
            return "Not enough Plants to perform payment";
        }

        return null;
    }
}
