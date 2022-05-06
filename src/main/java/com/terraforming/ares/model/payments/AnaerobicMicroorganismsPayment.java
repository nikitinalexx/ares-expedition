package com.terraforming.ares.model.payments;

import com.terraforming.ares.cards.blue.AnaerobicMicroorganisms;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.services.CardService;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public class AnaerobicMicroorganismsPayment extends GenericPayment {

    public AnaerobicMicroorganismsPayment(int value) {
        super(value);
    }

    @Override
    public PaymentType getType() {
        return PaymentType.ANAEROBIC_MICROORGANISMS;
    }

    @Override
    public int getTotalValue() {
        return getValue() * 5;
    }

    @Override
    public void pay(CardService deckService, PlayerContext player) {
        Integer resources = player.getCardResourcesCount().get(AnaerobicMicroorganisms.class);
        if (resources == null || resources < 2) {
            throw new IllegalStateException("Invalid payment: Anaerobic Microorganisms < 2");
        }
        player.getCardResourcesCount().put(AnaerobicMicroorganisms.class, resources - 2);
    }


}
