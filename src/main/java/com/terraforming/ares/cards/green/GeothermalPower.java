package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
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
public class GeothermalPower implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setHeatIncome(player.getHeatIncome() + 2);
        return null;
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
    public int getPrice() {
        return 8;
    }
}
