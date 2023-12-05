package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 04.12.2023
 */
@RequiredArgsConstructor
@Getter
public class CityPlanning implements InfrastructureExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CityPlanning(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("City Planning")
                .description("Raise infrastructure 2 steps.")
                .bonuses(List.of(Gain.of(GainType.INFRASTRUCTURE, 2)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getTerraformingService().increaseInfrastructure(marsContext);
        marsContext.getTerraformingService().increaseInfrastructure(marsContext);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 23;
    }

}
