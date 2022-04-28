package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.model.CorporationCard;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.Tag;

import java.util.Collections;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
public class LaunchStarIncorporated implements CorporationCard {
    @Override
    public void buildProject(PlayerContext playerContext) {
        playerContext.setMc(36);
    }

    @Override
    public String description() {
        return "MegaCredits: 36. At the start reveal deck cards until you find a blue one. Take it to your hand. Blue cards cost to you 3 less.";
    }

    @Override
    public Set<Tag> getTags() {
        return Collections.singleton(Tag.SCIENCE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }
}
