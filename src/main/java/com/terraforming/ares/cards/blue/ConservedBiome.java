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
 * Creation date 06.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ConservedBiome implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ConservedBiome(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Conserved Biome")
                .description("Add a microbe to ANOTHER* card or add an animal to ANOTHER* card. 1 VP per 2 forests you have.")
                .cardAction(CardAction.CONSERVED_BIOME)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public int getWinningPoints() {
        //TODO
        return 0;
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
        return List.of(Tag.BUILDING, Tag.MICROBE, Tag.ANIMAL);
    }

    @Override
    public int getPrice() {
        return 25;
    }
}
