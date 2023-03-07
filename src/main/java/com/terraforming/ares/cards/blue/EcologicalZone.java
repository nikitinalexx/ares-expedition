package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.winPoints.WinPointsInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class EcologicalZone implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public EcologicalZone(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Ecological Zone")
                .description("When you play a Animal or Plant, including these, add an animal to this card. 1 VP per 2 animals on this card.")
                .cardAction(CardAction.ECOLOGICAL_ZONE)
                .winPointsInfo(WinPointsInfo.builder()
                        .type(CardCollectableResource.ANIMAL)
                        .resources(2)
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
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().initResources(this);
        return null;
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.ANIMAL;
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        long animalsToAddCount = marsContext.getCardService().countCardTags(card, Set.of(Tag.ANIMAL, Tag.PLANT), inputParams);

        if (animalsToAddCount == 0) {
            return;
        }

        marsContext.getCardResourceService().addResources(marsContext.getPlayer(), this, (int) animalsToAddCount);
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
        return List.of(Tag.PLANT, Tag.ANIMAL);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
