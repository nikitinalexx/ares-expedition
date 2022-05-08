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
public class Crater implements BaseExpansionRedCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getTerraformingService().revealOcean(marsContext.getGame(), marsContext.getPlayer());

        return null;
    }

    @Override
    public String description() {
        return "Flip an ocean tile.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.EVENT, Tag.EVENT, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 7;
    }

}
