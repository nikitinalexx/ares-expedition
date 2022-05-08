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
public class PhysicsComplex implements BlueCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().getCardResourcesCount().put(PhysicsComplex.class, 0);
        return null;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.SCIENCE, Tag.SCIENCE, Tag.SCIENCE, Tag.SCIENCE);
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.SCIENCE;
    }

    @Override
    public void onTemperatureChangedEffect(Player player) {
        player.getCardResourcesCount().put(PhysicsComplex.class, player.getCardResourcesCount().get(PhysicsComplex.class) + 1);
    }

    @Override
    public String description() {
        return "1 VP per 2 science resources on this card. When you raise the temperature, add 1 science resource to this card.";
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
        return List.of(Tag.SCIENCE, Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 5;
    }
}
