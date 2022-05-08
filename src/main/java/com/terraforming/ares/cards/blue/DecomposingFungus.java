package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class DecomposingFungus implements BlueCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().getCardResourcesCount().put(DecomposingFungus.class, 2);
        return null;
    }

    @Override
    public String description() {
        return "Action: Remove 1 animal or 1 microbe from one of your cards to gain 3 plants.";
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.MICROBE;
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
        return Collections.singletonList(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
