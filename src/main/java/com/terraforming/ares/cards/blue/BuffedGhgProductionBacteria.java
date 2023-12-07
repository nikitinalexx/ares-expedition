package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.action.ActionInputDataType;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class BuffedGhgProductionBacteria implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BuffedGhgProductionBacteria(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("GHG Production Bacteria")
                .actionDescription("Action: Add 1 microbe to this card, or remove 2 microbes to raise the temperature 1 step.")
                .description("Add 2 microbes to this card.")
                .bonuses(List.of(Gain.of(GainType.MICROBE, 2)))
                .cardAction(CardAction.BUFFED_GHG_PRODUCTION)
                .actionInputData(
                        ActionInputData.builder()
                                .type(ActionInputDataType.ADD_DISCARD_MICROBE)
                                .min(1)
                                .max(2)
                                .build()
                )
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.MICROBE;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().initResources(this);

        marsContext.getCardResourceService().addResources(
                marsContext.getPlayer(), this, 2
        );

        return null;
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
    public List<ParameterColor> getOxygenRequirement() {
        return List.of(ParameterColor.R, ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
