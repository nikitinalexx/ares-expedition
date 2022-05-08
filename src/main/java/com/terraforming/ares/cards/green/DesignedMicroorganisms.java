package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
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
public class DesignedMicroorganisms implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlantsIncome(player.getPlantsIncome() + 2);

        return null;
    }

    @Override
    public String description() {
        return "During the production phase this produces 2 plants.";
    }

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.PURPLE, ParameterColor.RED);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
