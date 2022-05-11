package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AdvancedAlloys implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AdvancedAlloys(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Advanced Alloys")
                .description("Each titanium you have reduces the cost of Space cards an additional 1 MC. Each steel you have reduces the cost of Building cards an additional 1 MC.")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.BUILDING);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Collections.singleton(SpecialEffect.ADVANCED_ALLOYS);
    }

    @Override
    public int getPrice() {
        return 9;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }
}
