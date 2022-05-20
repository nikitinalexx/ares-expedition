package com.terraforming.ares.model.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import lombok.EqualsAndHashCode;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
@EqualsAndHashCode()
public class RestructuredResourcesPayment implements Payment {


    @Override
    public PaymentType getType() {
        return PaymentType.RESTRUCTURED_RESOURCES;
    }

    @Override
    public int getDiscount() {
        return 5;
    }

    @Override
    @JsonIgnore
    public int getValue() {
        return 0;
    }


    @Override
    public void pay(CardService deckService, Player player) {
        player.setPlants(player.getPlants() - 1);
    }


}
