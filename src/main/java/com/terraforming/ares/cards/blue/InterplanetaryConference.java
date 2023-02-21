package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
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
public class InterplanetaryConference implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public InterplanetaryConference(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Interplanetary Conference")
                .description("When you play an Earth or Jupiter tag, excluding this, you pay 3 MC less and draw a card.")
                .cardAction(CardAction.INTERPLANETARY_CONFERENCE)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.INTERPLANETARY_CONFERENCE);
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
        int cardsToGiveCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.EARTH, Tag.JUPITER), inputParams);

        if (cardsToGiveCount == 0) {
            return;
        }

        for (Integer card : marsContext.getCardService().dealCards(marsContext.getGame(), cardsToGiveCount)) {
            marsContext.getPlayer().getHand().addCard(card);
        }
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
        return List.of(Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 6;
    }
}
