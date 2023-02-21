package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.winPoints.WinPointsInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Livestock implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Livestock(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Livestock")
                .description("Requires yellow oxygen or higher. When you raise the temperature, add 1 animal to this card. 1 VP per animal on this card.")
                .cardAction(CardAction.LIVESTOCK)
                .winPointsInfo(WinPointsInfo.builder()
                        .type(CardCollectableResource.ANIMAL)
                        .resources(1)
                        .points(1)
                        .build()
                )
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void onTemperatureChangedEffect(Player player) {
        player.addResources(this, 1);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().initResources(this);
        return null;
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.ANIMAL;
    }

    @Override
    public List<ParameterColor> getOxygenRequirement() {
        return List.of(ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public int getWinningPoints() {
        //TODO
        return 0;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.ANIMAL);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
