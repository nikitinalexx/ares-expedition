package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2022
 */
@RequiredArgsConstructor
@Getter
public class ExperimentalTechnology implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ExperimentalTechnology(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Experimental Technology")
                .description("Action: Spend 1 TR to upgrade a phase card.")
                .cardAction(CardAction.EXPERIMENTAL_TECHNOLOGY)
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
        return List.of(Tag.SCIENCE, Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 4;
    }
}
