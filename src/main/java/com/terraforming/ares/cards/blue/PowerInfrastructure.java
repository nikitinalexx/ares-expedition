package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Tag;
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
public class PowerInfrastructure implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public PowerInfrastructure(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Power Infrastructure")
                .description("Action: Spend any amount of heat to gain that amount of MC.")
                .cardAction(CardAction.POWER_INFRASTRUCTURE)
                .actionInputData(
                        ActionInputData.builder()
                                .type(ActionInputDataType.DISCARD_HEAT)
                                .min(1)
                                .max(Integer.MAX_VALUE)
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
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 4;
    }
}
