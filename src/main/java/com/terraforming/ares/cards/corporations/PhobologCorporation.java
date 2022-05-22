package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;

import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
@Getter
public class PhobologCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE);
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.PHOBOLOG);
    }

    public PhobologCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Phobolog")
                .description("20 Mc. 1 Titanium income. Each Titanium you have is worth 1 MC extra.")
                .cardAction(CardAction.PHOBOLOG_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }


    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(20);
        player.setTitaniumIncome(player.getTitaniumIncome() + 1);
        return null;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

}
