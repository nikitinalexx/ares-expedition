package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.TerraformingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class GiantIceAsteroid implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public GiantIceAsteroid(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Giant Ice Asteroid")
                .description("Raise the temperature 2 steps. Flip 2 ocean tiles.")
                .bonuses(List.of(Gain.of(GainType.TEMPERATURE, 2), Gain.of(GainType.OCEAN, 2)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();

        for (int i = 0; i < 2; i++) {
            terraformingService.increaseTemperature(marsContext);
            terraformingService.revealOcean(marsContext);
        }

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 36;
    }

}
