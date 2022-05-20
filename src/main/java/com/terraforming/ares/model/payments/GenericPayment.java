package com.terraforming.ares.model.payments;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
@RequiredArgsConstructor
@Setter
@EqualsAndHashCode
public abstract class GenericPayment implements Payment{
    private final int value;

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public int getDiscount() {
        return 0;
    }


}
