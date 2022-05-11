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
public class AdvancedEcosystems implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AdvancedEcosystems(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Advanced Ecosystems")
                .description("Requires an Animal, Microbe, and Plant tags.")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.PLANT, Tag.MICROBE, Tag.ANIMAL);
    }

    @Override
    public int getWinningPoints() {
        return 3;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT, Tag.MICROBE, Tag.ANIMAL);
    }

    @Override
    public int getPrice() {
        return 10;
    }

}
