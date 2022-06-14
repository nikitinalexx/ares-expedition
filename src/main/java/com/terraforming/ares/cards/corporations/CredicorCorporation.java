package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
@Getter
public class CredicorCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.CREDICOR_DISCOUNT);
    }

    public CredicorCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Credicor")
                .description("48 Mc. When you play a card with a printed cost of 20 or more, get a 4 MC discount.")
                .cardAction(CardAction.CREDICOR_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(48);
        return null;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public int getPrice() {
        return 48;
    }
}
