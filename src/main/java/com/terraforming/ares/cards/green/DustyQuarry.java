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
public class DustyQuarry implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setSteelIncome(player.getSteelIncome() + 1);

        return null;
    }

    @Override
    public String description() {
        return "When you play a Building, you pay 2 MC less for it.";
    }

    @Override
    public LongPredicate getOceanRequirement() {
        return currentNumberOfOceans -> currentNumberOfOceans <= 3;
    }

    @Override
    public int getPrice() {
        return 2;
    }
}
