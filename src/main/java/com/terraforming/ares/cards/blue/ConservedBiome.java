package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ConservedBiome implements BlueCard {
    private final int id;

    @Override
    public String description() {
        return "1 VP for 2 Forests you have. " +
                "Action: Add a microbe to ANOTHER* card or add an animal to ANOTHER* card.";
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
        return List.of(Tag.BUILDING, Tag.MICROBE, Tag.ANIMAL);
    }

    @Override
    public int getPrice() {
        return 25;
    }
}
