package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 02.01.2026
 */
@RequiredArgsConstructor
@Getter
public class SyntheticCatastropheDummy implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public SyntheticCatastropheDummy(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Synthetic Catastrophe")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 0;
    }

}
