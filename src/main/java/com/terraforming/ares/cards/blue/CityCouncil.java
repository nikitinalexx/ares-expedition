package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@RequiredArgsConstructor
@Getter
public class CityCouncil implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CityCouncil(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("City Council")
                .description("Action: Draw a card. Draw an additional card for each Milestone you have")
                .cardAction(CardAction.CITY_COUNCIL)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 21;
    }
}
