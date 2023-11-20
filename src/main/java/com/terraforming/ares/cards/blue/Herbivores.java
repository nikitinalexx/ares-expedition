package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.OceanRequirement;
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
public class Herbivores implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Herbivores(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Herbivores")
                .description("Requires 5 oceans to be flipped. When you raise oxygen, flip an ocean tile, or raise temperature, add 1 animal to this card. 1 VP per 2 animals on this card.")
                .cardAction(CardAction.HERBIVORES)
                .winPointsInfo(WinPointsInfo.builder()
                        .type(CardCollectableResource.ANIMAL)
                        .resources(2)
                        .points(1)
                        .build()
                )
                .build();
    }

    @Override
    public int getWinningPoints() {
        return 0;
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
    public OceanRequirement getOceanRequirement() {
        return OceanRequirement.builder().minValue(5).maxValue(Constants.MAX_OCEANS).build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
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
    public List<Tag> getTags() {
        return List.of(Tag.ANIMAL);
    }

    @Override
    public void onOxygenChangedEffect(MarsContext context) {
        addAnimal(context);
    }

    @Override
    public void onTemperatureChangedEffect(MarsContext context) {
        addAnimal(context);
    }

    @Override
    public void onOceanFlippedEffect(MarsContext context) {
        addAnimal(context);
    }

    private void addAnimal(MarsContext context) {
        context.getCardResourceService().addResources(context.getPlayer(), this, 1);
    }

    @Override
    public int getPrice() {
        return 25;
    }
}
