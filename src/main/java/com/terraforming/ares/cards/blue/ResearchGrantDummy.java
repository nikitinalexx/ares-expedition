package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.01.2026
 */
@RequiredArgsConstructor
@Getter
public class ResearchGrantDummy implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ResearchGrantDummy(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Research Grant Dummy")
                .cardAction(CardAction.CHOOSE_TAG)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }


    @Override
    public List<Tag> getTags() {
        return List.of(Tag.DYNAMIC);
    }


    @Override
    public int getPrice() {
        return 0;
    }

}
