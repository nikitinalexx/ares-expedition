package com.terraforming.ares.model.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public class MegacreditsPayment extends GenericPayment {

    @JsonCreator
    public MegacreditsPayment(@JsonProperty("value") int value) {
        super(value);
    }

    @Override
    public PaymentType getType() {
        return PaymentType.MEGACREDITS;
    }

    @Override
    public void pay(CardService deckService, Player player) {
        player.setMc(player.getMc() - getValue());

        if (player.getMc() < 0) {
            throw new IllegalStateException("Invalid payment: mc < 0");
        }
    }

}
