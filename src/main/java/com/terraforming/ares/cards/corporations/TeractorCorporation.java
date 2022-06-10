package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
@Getter
public class TeractorCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.TERACTOR_EARTH_DISCOUNT);
    }

    public TeractorCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Teractor Corporation")
                .description("51 Mc. When you play an Earth tag get 3MC discount.")
                .cardAction(CardAction.TERACTOR_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(51);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.EARTH);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public int getPrice() {
        return 51;
    }

}
