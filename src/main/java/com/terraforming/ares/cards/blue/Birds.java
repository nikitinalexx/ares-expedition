package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Birds implements BlueCard{
    private final int id;

    @Override
    public String description() {
        return "Action: Add an animal to this card.";
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.ANIMAL;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().getCardResourcesCount().put(Birds.class, 0);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.ANIMAL);
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
    public List<ParameterColor> getOxygenRequirement() {
        return Collections.singletonList(ParameterColor.WHITE);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
