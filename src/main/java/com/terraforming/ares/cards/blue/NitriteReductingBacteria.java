package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.action.ActionInputDataType;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class NitriteReductingBacteria implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public NitriteReductingBacteria(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Nitrite Reducting Bacteria")
                .actionDescription("Action: Add 1 microbe to this card or remove 3 microbes to flip an ocean tile.")
                .description("Add 3 microbes to this card.")
                .bonuses(List.of(Gain.of(GainType.MICROBE, 3)))
                .cardAction(CardAction.NITRITE_REDUCTING)
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

        marsContext.getCardResourceService().addResources(
                marsContext.getPlayer(), this, 3
        );


        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
