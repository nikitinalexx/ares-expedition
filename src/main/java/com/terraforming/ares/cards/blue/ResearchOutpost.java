package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

import static com.terraforming.ares.model.SpecialEffect.RESEARCH_OUTPOST_DISCOUNT_1;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ResearchOutpost implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ResearchOutpost(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Research Outpost")
                .description("When you play a card, you pay 1 MC less for it.")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.RESEARCH_OUTPOST_DISCOUNT_1);
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
        return List.of(Tag.SCIENCE, Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 6;
    }
}
