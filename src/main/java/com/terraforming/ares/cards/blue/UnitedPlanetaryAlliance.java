package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.Expansion;
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
public class UnitedPlanetaryAlliance implements BlueCard {
    private final int id;

    @Override
    public String description() {
        //TODO 5 phase
        return "When you draw cards during the research phase, draw one additional card and keep one additional card.";
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
        return List.of(Tag.SCIENCE, Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
