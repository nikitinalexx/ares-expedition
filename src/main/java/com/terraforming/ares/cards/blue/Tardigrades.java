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
public class Tardigrades implements BlueCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().getCardResourcesCount().put(Tardigrades.class, 0);
        return null;
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.MICROBE;
    }

    @Override
    public String description() {
        return "1 VP per 3 microbes on this card.";
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
        return List.of(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 6;
    }
}
