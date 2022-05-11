package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class EarthCatapult implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public EarthCatapult(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Earth Catapult")
                .description("When you play a card, you pay 2 MC less for it.")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.EARTH_CATAPULT_DISCOUNT_2);
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 24;
    }
}
