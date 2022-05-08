package com.terraforming.ares.cards.red;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Plantation implements BaseExpansionRedCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        for (int i = 0; i < 2; i++) {
            marsContext.getTerraformingService().buildForest(marsContext.getGame(), marsContext.getPlayer());
        }
        return null;
    }

    @Override
    public String description() {
        return "Build 2 forests";
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.SCIENCE, Tag.SCIENCE, Tag.SCIENCE, Tag.SCIENCE);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 22;
    }

}
