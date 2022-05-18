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
    private final CardMetadata cardMetadata;

    public CelestiorCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Celestior Corporation")
                .description("50 Mc. Blue action bonus that is not implemented yet")
                .cardAction(CardAction.CELESTIOR_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().setMc(500);
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
