package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.InfrastructureExpansionBlueCard;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by oleksii.nikitin
 * Creation date 03.12.2023
 */
@RequiredArgsConstructor
@Getter
public class MaglevTrains implements InfrastructureExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MaglevTrains(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Maglev Trains")
                .description("Draw a card. If infrastructure was yellow or higher at the phase start, draw an additional card.")
                .cardAction(CardAction.MAGLEV_TRAINS)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public int getPrice() {
        return 16;
    }
}
