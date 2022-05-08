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
public class DiversifiedInterests implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlantsIncome(player.getPlantsIncome() + 1);
        player.setPlants(player.getPlants() + 3);
        player.setHeat(player.getHeat() + 3);

        return null;
    }

    @Override
    public String description() {
        return "Gain 3 plants and 3 heat. During the production phase, this produces 1 plant.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
