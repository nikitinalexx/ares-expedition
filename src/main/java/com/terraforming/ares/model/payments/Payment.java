package com.terraforming.ares.model.payments;

import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.services.DeckService;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public interface Payment {
    PaymentType getType();

    int getValue();
    int getTotalValue();

    void pay(DeckService deckService, PlayerContext player);
}
