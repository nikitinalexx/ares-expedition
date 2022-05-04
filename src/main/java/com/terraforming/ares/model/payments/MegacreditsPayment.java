package com.terraforming.ares.model.payments;

import com.terraforming.ares.model.PlayerContext;

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

    @Override
    public void pay(PlayerContext player) {
        player.setMc(player.getMc() - getValue());

        if (player.getMc() < 0) {
            throw new IllegalStateException("Invalid payment: mc < 0");
        }
    }

}
