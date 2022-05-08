package com.terraforming.ares.cards.red;

import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class InventionContest implements BaseExpansionRedCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Deck deck = marsContext.getGame().getProjectsDeck().dealCards(3);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : deck.getCards()) {
            marsContext.getPlayer().getHand().addCard(card);

            ProjectCard projectCard = marsContext.getCardService().getProjectCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        marsContext.getPlayer().setNextTurn(
                new DiscardCardsTurn(
                        marsContext.getPlayer().getUuid(),
                        new ArrayList<>(deck.getCards()),
                        2,
                        true
                )
        );

        return resultBuilder.build();
    }

    @Override
    public String description() {
        //TODO write test
        return "Draw 3 cards. Keep one of them and discard the other two.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 1;
    }

}
