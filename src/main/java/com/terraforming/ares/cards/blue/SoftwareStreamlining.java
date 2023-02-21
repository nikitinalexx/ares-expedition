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
public class SoftwareStreamlining implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public SoftwareStreamlining(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Software Streamlining")
                .actionDescription("Action: Draw two cards, then discard two cards.")
                .description("Upgrade a phase card.")
                .cardAction(CardAction.SOFTWARE_STREAMLINING)
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
        return List.of(Tag.SCIENCE, Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 14;
    }
}
