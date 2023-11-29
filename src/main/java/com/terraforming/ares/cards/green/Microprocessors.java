package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Microprocessors implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Microprocessors(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Microprocessors")
                .description("Draw 2 cards. Then, discard a card. During the production phase, this produces 3 heat.")
                .incomes(List.of(Gain.of(GainType.HEAT, 3)))
                .cardAction(CardAction.CAPITALISE_DESCRIPTION)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setHeat(player.getHeat() + 3);
    }

    @Override
    public boolean canPayAgain() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setHeatIncome(player.getHeatIncome() + 3);

        player.addNextTurn(
                new DiscardCardsTurn(
                        player.getUuid(),
                        List.of(),
                        1,
                        false,
                        true
                )
        );

        return marsContext.dealCards(2);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 17;
    }
}
