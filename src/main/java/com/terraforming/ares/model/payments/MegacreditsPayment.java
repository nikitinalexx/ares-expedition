package com.terraforming.ares.model.payments;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public class MegacreditsPayment extends GenericPayment {

    public MegacreditsPayment(int value) {
        super(value);
    }

    @Override
    public PaymentType getType() {
        return PaymentType.MEGACREDITS;
    }

}
