package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.action.ActionInputDataType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class HiTechLab implements ExperimentExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public HiTechLab(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Hi-Tech Lab")
                .description("Action: Spend X heat to draw X cards. Take 1 and discard other.")
                .cardAction(CardAction.HITECH_LAB)
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
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.BUILDING);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public int getPrice() {
        return 17;
    }

}
