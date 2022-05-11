package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.Tag;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Getter
public class AdaptationTechnology implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AdaptationTechnology(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Adaptation Technology")
                .description("When playing a card with requirements, you may consider the oxygen or temperature one color higher or lower. This cannot be modified futher by other effects.")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.SCIENCE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public int getPrice() {
        return 12;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Collections.singleton(SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT);
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }
}
