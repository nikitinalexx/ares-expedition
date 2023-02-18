package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
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
public class AutomatedFactories implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AutomatedFactories(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Automated Factories")
                .description("You may play a green card from your hand that has a printed cost of 9 MC or less without paying its MC cost. During the production phase, draw a card.")
                .incomes(List.of(Gain.of(GainType.CARD, 1)))
                .cardAction(CardAction.AUTOMATED_FACTORIES)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.getHand().addCards(cardService.dealCards(game, 1));
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setCardIncome(player.getCardIncome() + 1);
        player.setCanBuildInFirstPhase(player.getCanBuildInFirstPhase() + 1);
        player.setCanBuildAnotherGreenWith9Discount(true);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 18;
    }
}
