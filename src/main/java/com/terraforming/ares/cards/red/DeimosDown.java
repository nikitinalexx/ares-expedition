package com.terraforming.ares.cards.red;

import com.terraforming.ares.model.MarsContext;
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
public class DeimosDown implements BaseExpansionRedCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();
        for (int i = 1; i <= 3; i++) {
            terraformingService.increaseTemperature(marsContext.getGame(), marsContext.getPlayer());
        }

        marsContext.getPlayer().setMc(marsContext.getPlayer().getMc() + 7);

        return null;
    }

    @Override
    public String description() {
        return "Raise the temperature 3 steps. Gain 7 MC";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 30;
    }

}
