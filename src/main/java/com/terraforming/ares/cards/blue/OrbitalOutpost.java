package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@RequiredArgsConstructor
@Getter
public class OrbitalOutpost implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public OrbitalOutpost(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Orbital Outpost")
                .description("When you play a card with 1 or fewer tags, you pay 3 MC less for it.")
                .cardAction(CardAction.ORBITAL_OUTPOST)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.ORBITAL_OUTPOST_DISCOUNT);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 16;
    }
}
