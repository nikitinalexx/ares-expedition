package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 04.12.2023
 */
@RequiredArgsConstructor
@Getter
public class UrbanForestry implements InfrastructureExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public UrbanForestry(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Urban Forestry")
                .description("Requires yellow infrastructure or higher. Build a forest. Gain 5 MC.")
                .bonuses(List.of(Gain.of(GainType.FOREST, 1), Gain.of(GainType.MC, 5)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getTerraformingService().buildForest(marsContext);

        Player player = marsContext.getPlayer();
        player.setMc(player.getMc() + 5);

        return null;
    }

    @Override
    public List<ParameterColor> getInfrastructureRequirement() {
        return List.of(ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 12;
    }

}
