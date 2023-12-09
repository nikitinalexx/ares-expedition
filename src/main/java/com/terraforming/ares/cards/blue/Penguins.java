package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.ExperimentExpansionBlueCard;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.winPoints.WinPointsInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Penguins implements ExperimentExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Penguins(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Penguins")
                .description("Requires 8 oceans. Add an animal to this card. 1 VP per animal on this card.")
                .cardAction(CardAction.ADD_ANIMAL)
                .winPointsInfo(WinPointsInfo.builder()
                        .type(CardCollectableResource.ANIMAL)
                        .resources(1)
                        .points(1)
                        .build()
                )
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.ANIMAL;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().initResources(this);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.ANIMAL);
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public OceanRequirement getOceanRequirement() {
        return OceanRequirement.builder().minValue(8).maxValue(Constants.MAX_OCEANS).build();
    }

    @Override
    public int getPrice() {
        return 7;
    }

}
