package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class InterstellarColonyShip implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public InterstellarColonyShip(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Interstellar Colony Ship")
                .description("Requires 4 Science tag.")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.SCIENCE, Tag.SCIENCE, Tag.SCIENCE, Tag.SCIENCE);
    }

    @Override
    public int getWinningPoints() {
        return 4;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EARTH, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 20;
    }

}
