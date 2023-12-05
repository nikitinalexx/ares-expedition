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
public class ChpCombustionTurbines implements InfrastructureExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ChpCombustionTurbines(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("CHP Combustion Turbines")
                .description("Raise infrastructure 1 step. Raise oxygen 1 step. Raise temperature 1 step.")
                .bonuses(List.of(Gain.of(GainType.INFRASTRUCTURE, 1), Gain.of(GainType.OXYGEN, 1), Gain.of(GainType.TEMPERATURE, 1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getTerraformingService().increaseInfrastructure(marsContext);
        marsContext.getTerraformingService().raiseOxygen(marsContext);
        marsContext.getTerraformingService().increaseTemperature(marsContext);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.ENERGY, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 30;
    }

}
