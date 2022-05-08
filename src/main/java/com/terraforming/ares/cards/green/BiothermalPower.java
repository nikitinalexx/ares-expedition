package com.terraforming.ares.cards.green;

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
public class BiothermalPower implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        TerraformingService terraformingService = marsContext.getTerraformingService();

        player.setHeatIncome(player.getHeatIncome() + 1);
        terraformingService.buildForest(marsContext.getGame(), player);

        return null;
    }

    @Override
    public String description() {
        return "Build a forest. During the production phase, this produces 1 heat.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 18;
    }
}
