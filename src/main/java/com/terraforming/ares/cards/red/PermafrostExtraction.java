package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.ParameterColor;
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
public class PermafrostExtraction implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public PermafrostExtraction(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Permafrost Extraction")
                .description("Requires yellow temperature or warmer. Flip an ocean tile.")
                .bonuses(List.of(Gain.of(GainType.OCEAN, 1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();

        terraformingService.revealOcean(marsContext);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public int getPrice() {
        return 8;
    }

}
