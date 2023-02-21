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
public class OlympusConference implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public OlympusConference(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Olympus Conference")
                .description("When you play a Science tag, including this, draw a card.")
                .cardAction(CardAction.OLYMPUS_CONFERENCE)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        int scienceTags = marsContext.getCardService().countCardTags(project, Set.of(Tag.SCIENCE), inputParams);

        if (scienceTags == 0) {
            return;
        }

        for (Integer card : marsContext.getCardService().dealCards(marsContext.getGame(), scienceTags)) {
            marsContext.getPlayer().getHand().addCard(card);
        }

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
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.BUILDING, Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
