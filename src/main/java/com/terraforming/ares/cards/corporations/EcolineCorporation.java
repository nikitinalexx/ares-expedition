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
public class EcolineCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.ECOLINE_DISCOUNT);
    }

    public EcolineCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Ecoline")
                .description("27 Mc. 1 plant production. When you exchange plants for forest, pay 1 plant less.")
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
        player.setMc(27);
        player.setPlantsIncome(1);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public int getPrice() {
        return 27;
    }
}
