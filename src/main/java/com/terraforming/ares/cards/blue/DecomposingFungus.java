package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

import static com.terraforming.ares.model.action.ActionInputDataType.MICROBE_ANIMAL_CARD;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class DecomposingFungus implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public DecomposingFungus(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Decomposing Fungus")
                .description("Place 2 microbes on this card. Action: Remove 1 animal or 1 microbe from one of your cards to gain 3 plants.")
                .bonuses(List.of(Gain.of(GainType.MICROBE, 2)))
                .cardAction(CardAction.DECOMPOSING_FUNGUS)
                .actionInputData(
                        ActionInputData.builder()
                                .type(MICROBE_ANIMAL_CARD)
                                .min(1)
                                .max(1)
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
        marsContext.getPlayer().getCardResourcesCount().put(DecomposingFungus.class, 2);
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
        return Collections.singletonList(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
