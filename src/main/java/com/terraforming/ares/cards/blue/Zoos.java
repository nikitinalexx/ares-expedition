package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.model.winPoints.WinPointsInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Zoos implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Zoos(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Zoos")
                .description("When you gain a Milestone, add 1 animal to this card. 1 VP per animal on this card.")
                .cardAction(CardAction.ZOOS)
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
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().initResources(this);
        return null;
    }

    @Override
    public void onMilestoneGained(MarsContext context, Player player, Milestone milestone) {
        context.getCardResourceService().addResources(player, this, 1);
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
        return 7;
    }
}
