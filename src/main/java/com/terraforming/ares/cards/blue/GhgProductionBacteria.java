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
public class GhgProductionBacteria implements BlueCard {
    private final int id;

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.MICROBE;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().getCardResourcesCount().put(GhgProductionBacteria.class, 0);
        return null;
    }

    @Override
    public String description() {
        return "Add 1 microbe to this card, or remove 2 microbes to raise the temperature 1 step.";
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
        return List.of(ParameterColor.RED, ParameterColor.YELLOW, ParameterColor.WHITE);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
