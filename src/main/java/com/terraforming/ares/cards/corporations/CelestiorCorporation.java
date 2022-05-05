package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.model.CorporationCard;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.Tag;
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
public class CelestiorCorporation implements CorporationCard {
    private final int id;

    @Override
    public void buildProject(PlayerContext playerContext) {
        playerContext.setMc(50);
    }

    @Override
    public String description() {
        return "MegaCredits: 50. Action: reveal top three cards of the deck and take all event cards.";
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.SPACE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }
}
