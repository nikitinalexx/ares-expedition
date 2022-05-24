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
public class TropicalResort implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public TropicalResort(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Tropical Resort")
                .description("Requires you to spend 5 heat. During the production phase, this produces 4 МС.")
                .incomes(List.of(Gain.of(GainType.MC, 4)))
                .bonuses(List.of(Gain.of(GainType.HEAT, -5)))
                .cardAction(CardAction.TROPICAL_ISLAND)
                .build();
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public int heatSpendOnBuild() {
        return 5;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + 4);
        player.setHeat(player.getHeat() - 5);

        return null;
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

    @Override
    public int getPrice() {
        return 19;
    }
}
