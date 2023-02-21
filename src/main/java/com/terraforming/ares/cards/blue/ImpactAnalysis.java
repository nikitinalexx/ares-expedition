package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2023
 */
@RequiredArgsConstructor
@Getter
public class ImpactAnalysis implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ImpactAnalysis(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Impact Analysis")
                .description("Effect: When you play an Event tag, draw a card.")
                .cardAction(CardAction.IMPACT_ANALYSIS)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return false;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        int cardsToGiveCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.EVENT), inputParams);

        if (cardsToGiveCount == 0) {
            return;
        }

        for (Integer card : marsContext.getCardService().dealCards(marsContext.getGame(), cardsToGiveCount)) {
            marsContext.getPlayer().getHand().addCard(card);
        }
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
