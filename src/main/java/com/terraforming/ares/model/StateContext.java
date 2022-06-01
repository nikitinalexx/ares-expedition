package com.terraforming.ares.model;

import com.terraforming.ares.services.PaymentValidationService;
import com.terraforming.ares.services.TurnTypeService;
import lombok.Builder;
import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 01.06.2022
 */
@Value
@Builder
public class StateContext {
    String playerUuid;
    PaymentValidationService paymentValidationService;
    TurnTypeService turnTypeService;
}
