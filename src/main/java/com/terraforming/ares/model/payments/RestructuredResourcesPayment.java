package com.terraforming.ares.model.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public class RestructuredResourcesPayment extends GenericPayment {

    public RestructuredResourcesPayment() {
        super(1);
    }

    @JsonCreator
    public RestructuredResourcesPayment(@JsonProperty("value") int value) {
        super(value);
    }

    @Override
    public PaymentType getType() {
        return PaymentType.RESTRUCTURED_RESOURCES;
    }

    @Override
    public int getTotalValue() {
        return 5;
    }

    @Override
    public void pay(CardService deckService, Player player) {
        player.setPlants(player.getPlants() - 1);
    }


}
