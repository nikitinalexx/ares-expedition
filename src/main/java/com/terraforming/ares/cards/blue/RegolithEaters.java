package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.action.ActionInputDataType;
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
public class RegolithEaters implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public RegolithEaters(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Regolith Eaters")
                .description("Requires red temperature or warmer. Action: Add 1 microbe to this card, or remove 2 microbes from this card to raise oxygen 1 step.")
                .cardAction(CardAction.REGOLITH_EATERS)
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
        marsContext.getPlayer().getCardResourcesCount().put(RegolithEaters.class, 0);
        return null;
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
    public List<ParameterColor> getTemperatureRequirement() {
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
