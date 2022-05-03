package com.terraforming.ares.validation;

import com.terraforming.ares.model.PlayerContext;
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
    public String validate(PlayerContext playerContext, Payment payment) {
        if (playerContext.getMc() < payment.getValue()) {
            return "Not enough MC to build the project";
        }
        return null;
    }
}
