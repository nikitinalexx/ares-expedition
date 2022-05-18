package com.terraforming.ares.validation.payment;

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
public class MegacreditsPaymentValidator implements PaymentValidator {
    @Override
    public PaymentType getType() {
        return PaymentType.MEGACREDITS;
    }

    @Override
    public String validate(Card card, Player player, Payment payment) {
        if (player.getMc() < payment.getValue()) {
            return "Not enough MC to build the project";
        }
        return null;
    }
}
