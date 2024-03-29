package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
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
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Mangrove implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Mangrove(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Mangrove")
                .description("Requires white temperature. Build a Forest and raise oxygen 1 step.")
                .bonuses(List.of(Gain.of(GainType.FOREST, 1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getTerraformingService().buildForest(marsContext);

        return null;
    }

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.W);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 12;
    }

}
