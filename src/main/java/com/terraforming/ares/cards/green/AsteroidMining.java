package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AsteroidMining implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setTitaniumIncome(player.getTitaniumIncome() + 2);

        return null;
    }

    @Override
    public String description() {
        return "When you play a Space, you pay 6 MC less for it.";
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.JUPITER);
    }

    @Override
    public int getPrice() {
        return 28;
    }
}
