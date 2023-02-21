package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@RequiredArgsConstructor
@Getter
public class InterplanetaryCinematics implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.INTERPLANETARY_CINEMATICS_DISCOUNT);
    }

    public InterplanetaryCinematics(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Interplanetary Cinematics")
                .description("46 Mc. 1 steel production. When you play an Event, you pay 2 MC less for it.")
                .cardAction(CardAction.INTERPLANETARY_CINEMATICS)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(46);
        player.setSteelIncome(player.getSteelIncome() + 1);
        return null;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public int getPrice() {
        return 46;
    }
}
