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
public class FarmingCoops implements BlueCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setPlants(player.getPlants() + 3);
        return null;
    }

    @Override
    public String description() {
        return "Gain 3 plants when built. Action: Discard a card in hand to gain 3 plants.";
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT);
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
