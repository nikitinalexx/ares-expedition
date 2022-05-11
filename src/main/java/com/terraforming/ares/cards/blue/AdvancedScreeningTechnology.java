package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AdvancedScreeningTechnology implements BlueCard{
    private final int id;
    private final CardMetadata cardMetadata;

    public AdvancedScreeningTechnology(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Advanced Screening Tech")
                .description("Action: Reveal the top 3 cards of the deck. Place a card with a Science or Plant revealed this way into your hand. Discard the rest.")
                .cardAction(CardAction.SCREENING_TECHNOLOGY)
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
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public int getPrice() {
        return 6;
    }
}
