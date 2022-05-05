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
public class DevTechs implements CorporationCard {
    private final int id;

    @Override
    public void buildProject(PlayerContext playerContext) {
        playerContext.setMc(40);
    }

    @Override
    public String description() {
        return "MegaCredits: 40. Reveal 5 cards at the start of the game, take all green cards. Green cards cost for you 2 MC less.";
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.BUILDING);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }
}
