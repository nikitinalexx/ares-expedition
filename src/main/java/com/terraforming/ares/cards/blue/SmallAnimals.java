package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.*;
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
public class SmallAnimals implements BlueCard {
    private final int id;

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.RED, ParameterColor.YELLOW, ParameterColor.WHITE);
    }

    @Override
    public void onForestBuiltEffect(Player player) {
        player.getCardResourcesCount().put(SmallAnimals.class, player.getCardResourcesCount().get(SmallAnimals.class) + 1);
    }

    @Override
    public String description() {
        return "1 VP per 2 animals on this card. When you gain a forest VP, add 1 animal to this card.";
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
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().getCardResourcesCount().put(SmallAnimals.class, 0);
        return null;
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.ANIMAL;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.ANIMAL);
    }

    @Override
    public int getPrice() {
        return 9;
    }
}
