package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.cards.green.InfrastructureExpansionBlueCard;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 03.12.2023
 */
@RequiredArgsConstructor
@Getter
public class Sawmill implements InfrastructureExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Sawmill(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Sawmill")
                .description("Action: Spend 10 MC to raise infrastructure. Reduce this by 2 MC for every Plant tag you have.")
                .cardAction(CardAction.SAWMILL)
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
        return List.of(Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 18;
    }
}
