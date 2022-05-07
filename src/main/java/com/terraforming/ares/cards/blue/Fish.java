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
public class Fish implements BlueCard {
    private final int id;

    @Override
    public void onOceanFlippedEffect(Player player) {
        player.getCardResourcesCount().put(Fish.class, player.getCardResourcesCount().get(Fish.class) + 1);
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.ANIMAL;
    }

    @Override
    public void buildProject(Player player) {
        player.getCardResourcesCount().put(Fish.class, 0);
    }

    @Override
    public String description() {
        return "1 VP per animal on this card. Get 1 animal when you flip an ocean";
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
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.RED, ParameterColor.YELLOW, ParameterColor.WHITE);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.ANIMAL);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
