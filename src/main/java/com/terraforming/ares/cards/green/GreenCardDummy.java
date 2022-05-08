package com.terraforming.ares.cards.green;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class GreenCardDummy implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public String description() {
        return null;
    }

    @Override
    public int getPrice() {
        return 0;
    }
}
