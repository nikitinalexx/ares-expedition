package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.build.PutResourceOnBuild;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class CeosFavoriteProject implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CeosFavoriteProject(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("CEO's Favorite Project")
                .description("Add 2 resources to a card that holds resources.")
                .cardAction(CardAction.CAPITALISE_DESCRIPTION)
                .resourceOnBuild(
                        PutResourceOnBuild.builder()
                                .type(CardCollectableResource.ANY)
                                .count(1)
                                .paramId(InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId())
                                .build()
                )
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        List<Integer> cardInput = inputParams.get(InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId());

        int targetCardId = cardInput.get(0);

        Card targetCard = marsContext.getCardService().getCard(targetCardId);

        marsContext.getPlayer().addResources(targetCard, 2);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 3;
    }

}
