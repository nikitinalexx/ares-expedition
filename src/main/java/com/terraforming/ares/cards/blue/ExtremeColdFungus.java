package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ExtremeColdFungus implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ExtremeColdFungus(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Extreme-Cold Fungus")
                .description("Requires purple temperature. Gain 1 plant or add a microbe to ANOTHER* card.")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
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
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.PURPLE);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
