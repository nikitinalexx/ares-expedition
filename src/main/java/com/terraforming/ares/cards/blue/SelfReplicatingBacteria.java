package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.action.ActionInputDataType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class SelfReplicatingBacteria implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public SelfReplicatingBacteria(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Self-Replicating Bacteria")
                .description("Add a microbe to this card, or remove 5 microbes from this card to play a card from your hand. You pay 25 MC less for it.")
                .cardAction(CardAction.SELF_REPLICATING_BACTERIA)
                .actionInputData(
                        ActionInputData.builder()
                                .type(ActionInputDataType.ADD_DISCARD_MICROBE)
                                .min(1)
                                .max(5)
                                .build()
                )
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
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
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.MICROBE;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().initResources(this);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 8;
    }
}
