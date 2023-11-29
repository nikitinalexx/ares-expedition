package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
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
public class CommercialImports implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CommercialImports(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Commercial Imports")
                .description("During the production phase, this produces 1 Card, 2 Heat and 2 Plants.")
                .incomes(
                        List.of(
                                Gain.of(GainType.CARD, 1),
                                Gain.of(GainType.HEAT, 2),
                                Gain.of(GainType.PLANT, 2)
                        )
                )
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.getHand().addCards(cardService.dealCards(game, 1));
        player.setHeat(player.getHeat() + 2);
        player.setPlants(player.getPlants() + 2);
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
        player.setHeatIncome(player.getHeatIncome() + 2);
        player.setPlantsIncome(player.getPlantsIncome() + 2);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 36;
    }
}
