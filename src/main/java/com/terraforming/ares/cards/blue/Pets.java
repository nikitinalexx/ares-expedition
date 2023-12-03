package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.InfrastructureExpansionBlueCard;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.winPoints.WinPointsInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 03.12.2023
 */
@RequiredArgsConstructor
@Getter
public class Pets implements InfrastructureExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Pets(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Pets")
                .description("When you increase the infrastructure, add 1 animal to this card. 1 VP per 2 animals on this card.")
                .cardAction(CardAction.PETS)
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
    public void onInfrastructureChangedEffect(MarsContext context) {
        context.getCardResourceService().addResources(context.getPlayer(), this, 1);
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
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.ANIMAL;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.ANIMAL);
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
