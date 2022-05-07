package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Livestock implements BlueCard {
    private final int id;

    @Override
    public void onTemperatureChangedEffect(Player player) {
        player.getCardResourcesCount().put(
                Livestock.class,
                player.getCardResourcesCount().get(Livestock.class) + 1
        );
    }

    @Override
    public void buildProject(Player player) {
        player.getCardResourcesCount().put(Livestock.class, 0);
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.ANIMAL;
    }

    @Override
    public List<ParameterColor> getOxygenRequirement() {
        return List.of(ParameterColor.YELLOW, ParameterColor.WHITE);
    }

    @Override
    public String description() {
        return "1 VP per animal";
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.ANIMAL);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
