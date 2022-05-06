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
public class DevelopedInfrastructure implements BlueCard{
    private final int id;

    @Override
    public String description() {
        return "Action: Spend 10 MC to raise the temperature 1 step. Reduce this by 5 МС if you have 5 or more blue cards in play.";
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
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public int getPrice() {
        return 12;
    }
}
