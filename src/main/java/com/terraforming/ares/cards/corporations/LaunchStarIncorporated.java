package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@RequiredArgsConstructor
@Getter
public class LaunchStarIncorporated implements CorporationCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().setMc(36);
        return null;
    }

    @Override
    public String description() {
        return "MegaCredits: 36. At the start reveal deck cards until you find a blue one. Take it to your hand. Blue cards cost to you 3 less.";
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.SCIENCE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }
}
