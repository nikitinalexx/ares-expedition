package com.terraforming.ares.model.payments;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public class HeatPayment extends GenericPayment {

    public HeatPayment(int value) {
        super(value);
    }

    @Override
    public PaymentType getType() {
        return PaymentType.HEAT;
    }

}
