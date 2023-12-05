package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
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
public class GrainSilos implements InfrastructureExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public GrainSilos(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Grain Silos")
                .description("Raise infrastructure 2 steps. Gain 4 plants.")
                .bonuses(List.of(Gain.of(GainType.INFRASTRUCTURE, 2), Gain.of(GainType.PLANT, 4)))
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

        Player player = marsContext.getPlayer();
        player.setPlants(player.getPlants() + 4);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.PLANT, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 30;
    }

}
