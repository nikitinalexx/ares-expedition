package com.terraforming.ares.cards.red;

import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class LagrangeObservatory implements BaseExpansionRedCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Deck deck = marsContext.getGame().getProjectsDeck().dealCards(1);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : deck.getCards()) {
            marsContext.getPlayer().getHand().addCard(card);
            resultBuilder.takenCard(CardDto.from(marsContext.getCardService().getProjectCard(card)));
        }

        return resultBuilder.build();
    }

    @Override
    public String description() {
        return "Draw a card.";
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.SPACE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 7;
    }

}
