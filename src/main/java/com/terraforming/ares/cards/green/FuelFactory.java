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
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class FuelFactory implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public FuelFactory(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Fuel Factory")
                .description("Requires you to spend 3 heat. During the production phase, this produces 1 MC. When you play a Space, you pay 3 MC less for it.")
                .incomes(List.of(
                        Gain.of(GainType.MC, 1),
                        Gain.of(GainType.TITANIUM, 1)
                ))
                .cardAction(CardAction.FUEL_FACTORY)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + 1);
        player.setTitaniumIncome(player.getTitaniumIncome() + 1);
        player.setHeat(player.getHeat() - 3);

        return null;
    }

    @Override
    public int heatSpendOnBuild() {
        return 3;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 9;
    }
}
