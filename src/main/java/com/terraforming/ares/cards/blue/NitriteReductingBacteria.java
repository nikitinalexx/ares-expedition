package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class NitriteReductingBacteria implements BlueCard {
    private final int id;

    @Override
    public String description() {
        return "Add 3 microbes to this card. Action: Add 1 microbe to this card or remove 3 microbes to flip an ocean tile.";
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
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.MICROBE;
    }

    @Override
    public void buildProject(Player player) {
        player.getCardResourcesCount().put(NitriteReductingBacteria.class, 3);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
