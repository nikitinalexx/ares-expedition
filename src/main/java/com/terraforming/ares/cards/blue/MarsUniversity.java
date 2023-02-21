package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Getter
@RequiredArgsConstructor
public class MarsUniversity implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MarsUniversity(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Mars University")
                .description("When you play a Science tag, including this, you may discard a card. If that card had a Plant tag, draw two cards. Otherwise, draw a card.")
                .cardAction(CardAction.MARS_UNIVERSITY)
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
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        long scienceTagsCount = marsContext.getCardService().countCardTags(card, Set.of(Tag.SCIENCE), inputParams);

        if (scienceTagsCount == 0) {
            return;
        }

        List<Integer> cardsInput = inputParams.get(InputFlag.MARS_UNIVERSITY_CARD.getId());

        if (cardsInput.contains(InputFlag.SKIP_ACTION.getId())) {
            return;
        }

        for (Integer cardToDiscard : cardsInput) {
            discardCard(marsContext.getCardService(), cardToDiscard, marsContext.getGame(), marsContext.getPlayer());
        }
    }

    private void discardCard(CardService cardService, Integer cardIdToDiscard, MarsGame game, Player player) {
        player.getHand().removeCards(List.of(cardIdToDiscard));

        Card cardToDiscard = cardService.getCard(cardIdToDiscard);

        int cardsToReceive = cardToDiscard.getTags().contains(Tag.PLANT) || cardToDiscard.getTags().contains(Tag.DYNAMIC) ? 2 : 1;

        for (Integer dealedCard : cardService.dealCards(game, cardsToReceive)) {
            player.getHand().addCard(dealedCard);
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
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
