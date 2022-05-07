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
public class InterplanetaryRelations implements BlueCard {
    private final int id;

    @Override
    public String description() {
        //TODO phase 5 effect
        return "1 VP per 4 cards you have played. When you draw cards during the research phase, draw one additional card and keep one additional card.";
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
        return List.of(Tag.SPACE, Tag.JUPITER);
    }

    @Override
    public int getPrice() {
        return 35;
    }
}
