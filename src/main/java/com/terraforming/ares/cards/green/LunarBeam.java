package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class LunarBeam implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setTerraformingRating(player.getTerraformingRating() - 1);
        player.setHeatIncome(player.getHeatIncome() + 4);

        return null;
    }

    @Override
    public String description() {
        //TODO validation
        return "Requires you to spend 1 TR. During the production phase, this produces 4 heat.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 9;
    }
}
