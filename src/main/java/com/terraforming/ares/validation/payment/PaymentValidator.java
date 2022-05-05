package com.terraforming.ares.validation.payment;

import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.payments.PaymentType;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public interface PaymentValidator {

    PaymentType getType();

    String validate(PlayerContext playerContext, Payment payment);
}
