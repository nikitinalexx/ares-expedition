package com.terraforming.ares.cards.red;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.TerraformingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class TowingAComet implements BaseExpansionRedCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        TerraformingService terraformingService = marsContext.getTerraformingService();

        terraformingService.raiseTemperature(marsContext.getGame(), player);
        terraformingService.revealOcean(marsContext.getGame(), player);
        player.setPlants(player.getPlants() + 2);

        return null;
    }

    @Override
    public String description() {
        return "Raise oxygen 1 step. Flip an ocean tile. Gain 2 plants";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 22;
    }

}
