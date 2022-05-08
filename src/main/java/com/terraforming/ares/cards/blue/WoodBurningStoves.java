package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class WoodBurningStoves implements BlueCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setPlants(player.getPlants() + 4);
        return null;
    }

    @Override
    public String description() {
        return "Gain 4 plants. Action: Spend 4 plants to raise temperature 1 step. *if you chose the action phase this round, spend 3 plants instead.";
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 13;
    }
}
