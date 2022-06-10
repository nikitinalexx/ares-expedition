package com.terraforming.ares.cards.buffedCorporations;

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
public class BuffedPhobologCorporation implements CorporationCard {
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

    public BuffedPhobologCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Phobolog")
                .description("27 Mc. 1 Titanium income. 1 Space card. Each Titanium you have is worth 1 MC extra.")
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
        player.setMc(27);
        player.setTitaniumIncome(player.getTitaniumIncome() + 1);

        player.getHand().addCard(
                marsContext.getCardService().dealCardWithTag(Tag.SPACE, marsContext.getGame())
        );

        return null;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BUFFED_CORPORATION;
    }

    @Override
    public int getPrice() {
        return 27;
    }

}
