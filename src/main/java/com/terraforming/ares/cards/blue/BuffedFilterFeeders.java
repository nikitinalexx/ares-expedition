package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.OceanRequirement;
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
public class BuffedFilterFeeders implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BuffedFilterFeeders(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Filter Feeders")
                .description("When you add any number of microbes to ANOTHER* card, add an animal to this card. 1 VP per 2 animals on this card.")
                .cardAction(CardAction.FILTER_FEEDERS)
                .winPointsInfo(WinPointsInfo.builder()
                        .type(CardCollectableResource.ANIMAL)
                        .resources(2)
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
    public boolean isActiveCard() {
        return false;
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
    public Expansion getExpansion() {
        return Expansion.EXPERIMENTAL;
    }

    @Override
    public OceanRequirement getOceanRequirement() {
        return OceanRequirement.builder().minValue(2).maxValue(Constants.MAX_OCEANS).build();
    }

    @Override
    public int getPrice() {
        return 9;
    }
}
