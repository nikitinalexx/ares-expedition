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
 * Creation date 20.02.2023
 */
@RequiredArgsConstructor
@Getter
public class Dandelions implements DiscoveryExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Dandelions(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Dandelions")
                .description("Requires red temperature or warmer. During the production phase, draw a card and this produces 1 plant.")
                .incomes(List.of(Gain.of(GainType.CARD, 1), Gain.of(GainType.PLANT, 1)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.getHand().addCards(cardService.dealCards(game, 1));
        player.setPlants(player.getPlants() + 1);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.getHand().addCards(marsContext.getCardService().dealCards(marsContext.getGame(), 1));
        player.setPlantsIncome(player.getPlantsIncome() + 1);

        return null;
    }

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.R, ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 14;
    }

}
