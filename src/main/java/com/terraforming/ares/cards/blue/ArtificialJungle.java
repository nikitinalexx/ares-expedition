package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Tag;
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
public class ArtificialJungle implements BlueCard{
    private final int id;

    @Override
    public String description() {
        return "Action: Spend 1 plant to draw a card.";
    }

    @Override
    public List<Tag> getTags() {
        return Collections.emptyList();
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
    public int getPrice() {
        return 5;
    }
}
