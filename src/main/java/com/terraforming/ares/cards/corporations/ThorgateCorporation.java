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
public class ThorgateCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.TORGATE_ENERGY_DISCOUNT);
    }

    public ThorgateCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Thorgate Corporation")
                .description("45 Mc. 1 Heat income. When you play Energy tag get 3MC discount.")
                .cardAction(CardAction.THORGATE_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(45);
        player.setHeatIncome(1);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.ENERGY);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

}
