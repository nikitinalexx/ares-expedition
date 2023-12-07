package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class BusinessNetwork implements ExperimentExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BusinessNetwork(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Business Network")
                .description("Action: Spend 3 MC to draw a card.")
                .cardAction(CardAction.BUSINESS_NETWORK)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 4;
    }
}
