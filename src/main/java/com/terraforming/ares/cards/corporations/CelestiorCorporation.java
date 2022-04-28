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
public class CelestiorCorporation implements CorporationCard {

    @Override
    public void buildProject(PlayerContext playerContext) {
        playerContext.setMc(50);
    }

    @Override
    public String description() {
        return "MegaCredits: 50. Action: reveal top three cards of the deck and take all event cards.";
    }

    @Override
    public Set<Tag> getTags() {
        return Collections.singleton(Tag.SPACE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }
}
