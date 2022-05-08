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
public class GiantSpaceMirror implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setHeatIncome(player.getHeatIncome() + 3);

        return null;
    }

    @Override
    public String description() {
        return "During the production phase, this produces 3 heat.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 13;
    }
}
