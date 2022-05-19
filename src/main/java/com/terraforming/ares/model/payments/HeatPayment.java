package com.terraforming.ares.model.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import lombok.EqualsAndHashCode;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
@EqualsAndHashCode(callSuper = true)
public class HeatPayment extends GenericPayment {

    @JsonCreator
    public HeatPayment(@JsonProperty("value") int value) {
        super(value);
    }

    @Override
    public PaymentType getType() {
        return PaymentType.HEAT;
    }

    @Override
    public void pay(CardService deckService, Player player) {
        player.setHeat(player.getHeat() - getValue());

        if (player.getHeat() < 0) {
            throw new IllegalStateException("Invalid payment: heat < 0");
        }
    }

}
