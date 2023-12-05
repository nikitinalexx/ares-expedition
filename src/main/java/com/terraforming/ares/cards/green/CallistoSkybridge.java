package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 04.12.2023
 */
@RequiredArgsConstructor
@Getter
public class CallistoSkybridge implements InfrastructureExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CallistoSkybridge(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Callisto Skybridge")
                .description("Raise infrastructure 1 step. If you have 3 or more other Jupiter tags, raise infrastructure an additional 1 step.")
                .bonuses(List.of(Gain.of(GainType.INFRASTRUCTURE, 1)))
                .cardAction(CardAction.SKYBRIDGE)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getTerraformingService().increaseInfrastructure(marsContext);

        int jupiterCount = marsContext.getCardService().countPlayedTags(marsContext.getPlayer(), Set.of(Tag.JUPITER));

        if (jupiterCount >= 3) {
            marsContext.getTerraformingService().increaseInfrastructure(marsContext);
        }

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.JUPITER, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 17;
    }

}
