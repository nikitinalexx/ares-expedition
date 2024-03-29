package com.terraforming.ares.model.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terraforming.ares.cards.blue.AnaerobicMicroorganisms;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import lombok.EqualsAndHashCode;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
@EqualsAndHashCode
public class AnaerobicMicroorganismsPayment implements Payment {

    @Override
    @JsonIgnore
    public int getValue() {
        return 0;
    }

    @Override
    public int getDiscount() {
        return 10;
    }

    @Override
    public PaymentType getType() {
        return PaymentType.ANAEROBIC_MICROORGANISMS;
    }

    @Override
    public void pay(CardService deckService, Player player) {
        Card card = player.getPlayed()
                .getCards()
                .stream()
                .map(deckService::getCard).filter(c -> c.getClass() == AnaerobicMicroorganisms.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Invalid payment: card Anaerobic Microorganisms was not played"));

        Integer resources = player.getCardResourcesCount().get(card.getClass());
        if (resources == null || resources < 2) {
            throw new IllegalStateException("Invalid payment: Anaerobic Microorganisms < 2");
        }
        player.removeResources(card, 2);
    }


}
