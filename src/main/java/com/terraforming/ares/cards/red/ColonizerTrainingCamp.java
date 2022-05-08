package com.terraforming.ares.cards.red;

import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ColonizerTrainingCamp implements BaseExpansionRedCard {
    private final int id;

    @Override
    public String description() {
        return null;
    }

    @Override
    public List<ParameterColor> getOxygenRequirement() {
        return List.of(ParameterColor.PURPLE, ParameterColor.RED);
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.JUPITER, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 10;
    }

}
