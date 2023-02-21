package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.action.ActionInputDataType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@RequiredArgsConstructor
@Getter
public class FibrousCompositeMaterial implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public FibrousCompositeMaterial(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Fibrous Composite Material")
                .actionDescription("Action: Add 1 science to this card or remove 3 science to upgrade a phase.")
                .description("Add 3 science resources to this card.")
                .cardAction(CardAction.FIBROUS_COMPOSITE_MATERIAL)
                .actionInputData(
                        ActionInputData.builder()
                                .type(ActionInputDataType.ADD_DISCARD_MICROBE)
                                .min(1)
                                .max(3)
                                .build()
                )
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().initResources(this);
        marsContext.getPlayer().getCardResourcesCount().put(this.getClass(), 3);
        return null;
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.SCIENCE;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE);
    }

    @Override
    public int getPrice() {
        return 8;
    }
}
