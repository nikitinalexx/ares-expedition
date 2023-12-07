package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.action.ActionInputDataType;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 06.12.2023
 */
@RequiredArgsConstructor
@Getter
public class BuffedGreenHouses implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BuffedGreenHouses(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Greenhouses")
                .description("Requires yellow temperature or warmer. Action: Spend up to 4 heat to gain that amount of plants.")
                .cardAction(CardAction.GREEN_HOUSES)
                .actionInputData(
                        ActionInputData.builder()
                                .type(ActionInputDataType.DISCARD_HEAT)
                                .min(1)
                                .max(4)
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
        return Expansion.EXPERIMENTAL;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 11;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

}
