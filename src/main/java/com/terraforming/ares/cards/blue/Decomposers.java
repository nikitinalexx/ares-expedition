package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_CARD;
import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_MICROBE;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Decomposers implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Decomposers(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Decomposers")
                .description("Requires red oxygen or higher. When you play an Animal, Microbe, or Plant, including this, add a microbe here or remove a microbe from here to draw a card.")
                .cardAction(CardAction.DECOMPOSERS)
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
        return CardCollectableResource.MICROBE;
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
    public List<ParameterColor> getOxygenRequirement() {
        return List.of(ParameterColor.R, ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 7;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        long tagsCount = marsContext.getCardService().countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), inputParams);

        if (tagsCount == 0) {
            return;
        }

        final Player player = marsContext.getPlayer();

        if (inputParams.containsKey(DECOMPOSERS_TAKE_MICROBE.getId()) && !CollectionUtils.isEmpty(inputParams.get(DECOMPOSERS_TAKE_MICROBE.getId()))) {
            marsContext.getCardResourceService().addResources(player, this, inputParams.get(DECOMPOSERS_TAKE_MICROBE.getId()).get(0));
        }

        if (inputParams.containsKey(DECOMPOSERS_TAKE_CARD.getId()) && !CollectionUtils.isEmpty(inputParams.get(DECOMPOSERS_TAKE_CARD.getId()))) {
            Integer takeCardsCount = inputParams.get(DECOMPOSERS_TAKE_CARD.getId()).get(0);
            player.removeResources(this, takeCardsCount);

            for (Integer cardId : marsContext.getCardService().dealCards(marsContext.getGame(), takeCardsCount)) {
                player.getHand().addCard(cardId);
            }
        }
    }
}
