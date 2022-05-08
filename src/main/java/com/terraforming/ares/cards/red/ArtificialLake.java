package com.terraforming.ares.cards.red;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ArtificialLake implements BaseExpansionRedCard {
    private final int id;

    @Override
    public String description() {
        return "Flip an ocean tile.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getTerraformingService().revealOcean(marsContext.getGame(), marsContext.getPlayer());
        return null;
    }

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.YELLOW, ParameterColor.WHITE);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public int getPrice() {
        return 13;
    }

}
