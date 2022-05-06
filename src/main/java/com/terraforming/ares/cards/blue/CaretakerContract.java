package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@RequiredArgsConstructor
@Getter
public class CaretakerContract implements BlueCard {
    private final int id;

    @Override
    public String description() {
        return "Action: Spend 8 heat to raise your TR 1 step.";
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
    public List<ParameterColor> getTemperatureRequirement() {
        return Arrays.asList(ParameterColor.YELLOW, ParameterColor.WHITE);
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

    @Override
    public int getPrice() {
        return 18;
    }
}
