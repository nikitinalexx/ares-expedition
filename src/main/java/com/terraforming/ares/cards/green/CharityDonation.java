package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
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
public class CharityDonation implements ExperimentExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CharityDonation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Charity Donation")
                .description("Reveal cards from the deck equal to number of players + 1. In random order starting from you, everyone takes 1 card.")
                .cardAction(CardAction.CAPITALISE_DESCRIPTION)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int toDiscardCount = marsContext.getGame().getPlayerUuidToPlayer().size();
        List<Integer> cards = marsContext.getCardService().dealCards(marsContext.getGame(), toDiscardCount + 1);

        Player player = marsContext.getPlayer();

        for (Integer card : cards) {
            player.getHand().addCard(card);
        }

        player.addNextTurn(
                new DiscardCardsTurn(
                        player.getUuid(),
                        new ArrayList<>(cards),
                        toDiscardCount,
                        true,
                        true,
                        new ArrayList<>(List.of(player.getUuid()))
                )
        );

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public int getPrice() {
        return 7;
    }
}
