package com.terraforming.ares.validation.payment;

import com.terraforming.ares.cards.blue.AnaerobicMicroorganisms;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.payments.PaymentType;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
@Component
@RequiredArgsConstructor
public class AnaerobicMicroorganismsPaymentValidator implements PaymentValidator {
    private final CardService deckService;

    @Override
    public PaymentType getType() {
        return PaymentType.ANAEROBIC_MICROORGANISMS;
    }

    @Override
    public String validate(Player player, Payment payment) {
        if (payment.getValue() != 2) {
            return "Invalid payment: Anaerobic Microorganisms can be paid only with a value of 2";
        }

        Integer resources = player.getCardResourcesCount().get(AnaerobicMicroorganisms.class);
        if (resources == null || resources < 2) {
            return "Invalid payment: Anaerobic Microorganisms < 2";
        }

        return null;
    }
}
