package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
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
    private final CardMetadata cardMetadata;

    public InventionContest(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Invention Contest")
                .description("Draw 3 cards. Keep one of them and discard the other two.")
                .cardAction(CardAction.CAPITALISE_DESCRIPTION)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        List<Integer> cards = marsContext.getCardService().dealCards(marsContext.getGame(), 3);

        for (Integer card : cards) {
            marsContext.getPlayer().getHand().addCard(card);

            Card projectCard = marsContext.getCardService().getCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        marsContext.getPlayer().setNextTurn(
                new DiscardCardsTurn(
                        marsContext.getPlayer().getUuid(),
                        new ArrayList<>(cards),
                        2,
                        true
                )
        );

        return resultBuilder.build();
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
