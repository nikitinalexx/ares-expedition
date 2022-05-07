package com.terraforming.ares.model.payments;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public interface Payment {
    PaymentType getType();

    int getValue();
    int getTotalValue();

    void pay(CardService deckService, Player player);
}
