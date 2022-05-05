package com.terraforming.ares.model.payments;

import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.services.DeckService;

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

    @Override
    public void pay(DeckService deckService, PlayerContext player) {
        player.setHeat(player.getHeat() - getValue());

        if (player.getHeat() < 0) {
            throw new IllegalStateException("Invalid payment: heat < 0");
        }
    }

}
