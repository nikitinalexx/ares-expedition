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
public class MediaGroup implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MediaGroup(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Media Group")
                .description("When you play an Event, you pay 5 MC less for it.")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.MEDIA_GROUP);
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
    public List<Tag> getTags() {
        return List.of(Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
