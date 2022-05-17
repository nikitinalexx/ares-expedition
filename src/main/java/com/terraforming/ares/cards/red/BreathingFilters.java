package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class BreathingFilters implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BreathingFilters(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Breathing Filters")
                .description("Requires yellow oxygen or higher. ")
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
    public List<ParameterColor> getOxygenRequirement() {
        return List.of(ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

    @Override
    public int getPrice() {
        return 9;
    }

}
