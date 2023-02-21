package com.terraforming.ares.services;

import com.terraforming.ares.model.StateContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 01.06.2022
 */
@Service
@RequiredArgsConstructor
public class StateContextProvider {
    private final PaymentValidationService paymentValidationService;
    private final TurnTypeService turnTypeService;


    public StateContext createStateContext(String player) {
        return StateContext.builder()
                .playerUuid(player)
                .paymentValidationService(paymentValidationService)
                .turnTypeService(turnTypeService)
                .build();
    }
}
