package com.terraforming.ares.model.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MegacreditsPayment.class, name = "MEGACREDITS"),
        @JsonSubTypes.Type(value = HeatPayment.class, name = "HEAT"),
        @JsonSubTypes.Type(value = AnaerobicMicroorganismsPayment.class, name = "ANAEROBIC_MICROORGANISMS"),
        @JsonSubTypes.Type(value = RestructuredResourcesPayment.class, name = "RESTRUCTURED_RESOURCES")

})
public interface Payment {
    @JsonIgnore
    PaymentType getType();

    int getValue();

    @JsonIgnore
    int getDiscount();

    void pay(CardService deckService, Player player);
}
