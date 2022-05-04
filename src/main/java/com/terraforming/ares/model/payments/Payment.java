package com.terraforming.ares.model.payments;

import com.terraforming.ares.model.PlayerContext;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public interface Payment {
    PaymentType getType();
    int getValue();
    void pay(PlayerContext player);
}
