package com.terraforming.ares.model;

import com.terraforming.ares.services.PaymentValidationService;
import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 01.06.2022
 */
@Value
public class StateContext {
    String playerUuid;
    PaymentValidationService paymentValidationService;
}
