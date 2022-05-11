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
public class CelestiorCorporation implements CorporationCard {
    private final int id;

    @Override
    public CardMetadata getCardMetadata() {
        return null;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().setMc(50);
        return null;
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
