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
public class Farming implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlants(player.getPlants() + 2);
        player.setPlantsIncome(player.getPlantsIncome() + 2);
        player.setMcIncome(player.getMcIncome() + 2);

        return null;
    }

    @Override
    public String description() {
        return "Gain 2 plants. During the production phase, this produces 2 MC and 2 plants.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT);
    }

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.WHITE);
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

    @Override
    public int getPrice() {
        return 20;
    }
}
