package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 04.12.2023
 */
@RequiredArgsConstructor
@Getter
public class QuantLinkConferencing implements InfrastructureExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public QuantLinkConferencing(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("QuantLink Conferencing")
                .description("Requires red infrastructure or higher. During the production phase, draw a card.")
                .incomes(List.of(Gain.of(GainType.CARD, 1)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.getHand().addCards(cardService.dealCards(game, 1));
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

        player.setCardIncome(player.getCardIncome() + 1);

        return null;
    }

    @Override
    public List<ParameterColor> getInfrastructureRequirement() {
        return List.of(ParameterColor.R, ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE);
    }

    @Override
    public int getPrice() {
        return 13;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }
}
