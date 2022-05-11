package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.LongPredicate;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class DustyQuarry implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public DustyQuarry(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Dusty Quarry")
                .description("Requires 3 or fewer ocean tiles to be flipped. When you play a Building, you pay 2 MC less for it.")
                .incomes(List.of(Gain.of(GainType.STEEL, 1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setSteelIncome(player.getSteelIncome() + 1);

        return null;
    }

    @Override
    public LongPredicate getOceanRequirement() {
        return currentNumberOfOceans -> currentNumberOfOceans <= 3;
    }

    @Override
    public int getPrice() {
        return 2;
    }
}
