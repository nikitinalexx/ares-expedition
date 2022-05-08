package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.LongPredicate;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class KelpFarming implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlants(player.getPlants() + 2);
        player.setMcIncome(player.getMcIncome() + 2);
        player.setPlantsIncome(player.getPlantsIncome() + 3);

        return null;
    }

    @Override
    public String description() {
        return "Gain 2 plants. During the production phase, this produces 2 MC and 3 plants.";
    }

    @Override
    public LongPredicate getOceanRequirement() {
        //TODO
        return currentNumberOfOceans -> currentNumberOfOceans >= 6;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public int getPrice() {
        return 17;
    }
}
