package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.ExperimentExpansionGreenCard;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.parameters.OceanRequirement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class DustSeals implements ExperimentExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public DustSeals(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Dust Seals")
                .description("Requires 3 or less ocean tiles.")
                .cardAction(CardAction.CAPITALISE_DESCRIPTION)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public OceanRequirement getOceanRequirement() {
        return OceanRequirement.builder().minValue(0).maxValue(3).build();
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public int getPrice() {
        return 2;
    }

}
