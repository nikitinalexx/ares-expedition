package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.action.ActionInputDataType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class RedraftedContracts implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public RedraftedContracts(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Redrafted Contracts")
                .description("Action: Discard up to three cards in hand. Draw that many cards.")
                .cardAction(CardAction.REDRAFTED_CONTRACTS)
                .actionInputData(
                        ActionInputData.builder()
                                .type(ActionInputDataType.DISCARD_CARD)
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
    public int getPrice() {
        return 4;
    }
}
