package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
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
public class Tardigrades implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Tardigrades(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Tardigrades")
                .description("Action: Add 1 microbe to this card. 1 VP per 3 microbes on this card.")
                .cardAction(CardAction.ADD_MICROBE)
                .winPointsInfo(WinPointsInfo.builder()
                        .type(CardCollectableResource.MICROBE)
                        .resources(3)
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
        marsContext.getPlayer().getCardResourcesCount().put(Tardigrades.class, 0);
        return null;
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.MICROBE;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 6;
    }
}
