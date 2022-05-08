package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class TradingPost implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlants(player.getPlants() + 3);
        player.setMcIncome(player.getMcIncome() + 2);

        return null;
    }

    @Override
    public String description() {
        return "Gain 3 plants. During the production phase, this produces 2 МС.";
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
