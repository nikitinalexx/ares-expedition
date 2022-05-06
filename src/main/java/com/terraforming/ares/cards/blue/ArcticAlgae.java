package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ArcticAlgae implements BlueCard {
    private final int id;

    @Override
    public void onOceanFlippedEffect(PlayerContext player) {
        player.setPlants(player.getPlants() + 4);
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

    @Override
    public String description() {
        return "Requires red temperature or warmer. When you flip an ocean tile, gain 4 plants.";
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.PLANT);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return Arrays.asList(ParameterColor.RED, ParameterColor.YELLOW, ParameterColor.WHITE);
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public int getPrice() {
        return 19;
    }
}
