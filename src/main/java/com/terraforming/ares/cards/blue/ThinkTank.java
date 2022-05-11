package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ThinkTank implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ThinkTank(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Think Tank")
                .description("Action: Spend 2 MC to draw a card. 1 VP per 3 blue cards you have in play.")
                .cardAction(CardAction.THINKTANK)
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
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE);
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public int getPrice() {
        return 13;
    }
}
