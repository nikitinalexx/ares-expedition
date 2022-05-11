package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class PowerSupplyConsortium implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public PowerSupplyConsortium(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Power Supply Consortium")
                .description("During the production phase, this produces 2 MC and 1 heat.")
                .incomes(List.of(
                        Gain.of(GainType.MC, 2),
                        Gain.of(GainType.HEAT, 1)
                ))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + 2);
        player.setHeatIncome(player.getHeatIncome() + 1);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 12;
    }
}
