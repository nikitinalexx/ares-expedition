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
public class MatterGenerator implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MatterGenerator(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Matter Generator")
                .description("Draw 2 Cards. Action: Discard a card in hand to gain 6 MC.")
                .cardAction(CardAction.MATTER_GENERATOR)
                .actionInputData(
                        ActionInputData.builder()
                                .type(ActionInputDataType.DISCARD_CARD)
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
        return marsContext.dealCards(2);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public int getPrice() {
        return 13;
    }
}
