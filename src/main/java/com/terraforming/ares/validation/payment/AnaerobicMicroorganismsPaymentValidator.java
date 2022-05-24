package com.terraforming.ares.validation.payment;

import com.terraforming.ares.cards.blue.AnaerobicMicroorganisms;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.payments.PaymentType;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
@Component
public class AnaerobicMicroorganismsPaymentValidator implements PaymentValidator {

    @Override
    public PaymentType getType() {
        return PaymentType.ANAEROBIC_MICROORGANISMS;
    }

    @Override
    public String validate(Card card, Player player, Payment payment) {
        Integer resources = player.getCardResourcesCount().get(AnaerobicMicroorganisms.class);
        if (resources == null || resources < 2) {
            return "Invalid payment: Anaerobic Microorganisms < 2";
        }

        return null;
    }
}
