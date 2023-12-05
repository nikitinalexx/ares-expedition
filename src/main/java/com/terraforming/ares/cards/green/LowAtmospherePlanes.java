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
public class LowAtmospherePlanes implements InfrastructureExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public LowAtmospherePlanes(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("LowAtmosphere Planes")
                .description("Raise infrastructure 3 steps.")
                .bonuses(List.of(Gain.of(GainType.INFRASTRUCTURE, 3)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        for (int i = 0; i < 3; i++) {
            marsContext.getTerraformingService().increaseInfrastructure(marsContext);
        }

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 28;
    }

    @Override
    public int getWinningPoints() {
        return -1;
    }

}
