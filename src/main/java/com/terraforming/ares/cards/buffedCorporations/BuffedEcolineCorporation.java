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
public class BuffedEcolineCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.ECOLINE_DISCOUNT);
    }

    public BuffedEcolineCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Ecoline")
                .description("42 Mc. 1 plant production. 1 Plant card. When you exchange plants for forest, pay 1 plant less.")
                .cardAction(CardAction.ECOLINE_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(42);
        player.setPlantsIncome(1);

        player.getHand().addCard(
                marsContext.getCardService().dealCardWithTag(Tag.PLANT, marsContext.getGame())
        );

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BUFFED_CORPORATION;
    }

    @Override
    public int getPrice() {
        return 42;
    }

}
