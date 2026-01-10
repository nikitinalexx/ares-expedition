package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.build.PutResourceOnBuild;
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
public class CeosFavoriteProjectDummy implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CeosFavoriteProjectDummy(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("CEO's Favorite Project Dummy")
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
        return 3;
    }

}
