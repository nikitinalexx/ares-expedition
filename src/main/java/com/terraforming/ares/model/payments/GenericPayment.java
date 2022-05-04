package com.terraforming.ares.model.payments;

import lombok.RequiredArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
@RequiredArgsConstructor
public abstract class GenericPayment implements Payment{
    private final int value;

    @Override
    public int getValue() {
        return value;
    }


}
