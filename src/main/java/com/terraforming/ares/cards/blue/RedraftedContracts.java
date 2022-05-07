package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.Expansion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class RedraftedContracts implements BlueCard {
    private final int id;

    @Override
    public String description() {
        return "Discard up to three cards in hand. Draw that many cards.";
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public int getPrice() {
        return 4;
    }
}
