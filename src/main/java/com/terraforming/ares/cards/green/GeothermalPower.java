package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@RequiredArgsConstructor
@Getter
public class GeothermalPower implements GreenCard {
    private final int id;

    @Override
    public void buildProject(PlayerContext playerContext) {
        playerContext.setHeatIncome(playerContext.getHeatIncome() + 2);
    }

    @Override
    public String description() {
        return "During the production phase, this produces 2 heat.";
    }

    @Override
    public List<Tag> getTags() {
        return Arrays.asList(Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public int getPrice() {
        return 8;
    }
}
