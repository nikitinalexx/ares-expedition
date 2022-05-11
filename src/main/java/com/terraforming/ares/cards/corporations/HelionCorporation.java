package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
@RequiredArgsConstructor
@Getter
public class HelionCorporation implements CorporationCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(28);
        player.setHeatIncome(3);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.SPACE);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return null;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

}
