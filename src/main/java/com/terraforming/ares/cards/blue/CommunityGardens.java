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
 * Creation date 06.05.2022
 */
@RequiredArgsConstructor
@Getter
public class CommunityGardens implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CommunityGardens(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Community Gardens")
                .description("Action: Gain 2 MC. *if you chose the action phase this round, also gain 1 plant.")
                .cardAction(CardAction.COMMUNITY_GARDENS)
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
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 20;
    }
}
