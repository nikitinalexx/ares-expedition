package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
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
public class DevTechs implements CorporationCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().setMc(40);
        return null;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return null;
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
