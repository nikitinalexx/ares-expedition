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
public class PowerGridUplink implements InfrastructureExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public PowerGridUplink(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Power Grid Uplink")
                .description("Raise infrastructure 2 steps. Raise the temperature 1 step.")
                .bonuses(List.of(Gain.of(GainType.INFRASTRUCTURE, 2), Gain.of(GainType.TEMPERATURE, 1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        for (int i = 0; i < 2; i++) {
            marsContext.getTerraformingService().increaseInfrastructure(marsContext);
        }
        marsContext.getTerraformingService().increaseTemperature(marsContext);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.ENERGY, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 32;
    }

}
