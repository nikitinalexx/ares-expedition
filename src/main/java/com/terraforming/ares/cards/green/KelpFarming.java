package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.OceanRequirement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class KelpFarming implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public KelpFarming(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Kelp Farming")
                .description("Requires 6 ocean tiles to be flipped. Gain 2 plants. During the production phase, this produces 2 MC and 3 plants.")
                .incomes(List.of(
                        Gain.of(GainType.MC, 2),
                        Gain.of(GainType.PLANT, 3)
                ))
                .bonuses(List.of(Gain.of(GainType.PLANT, 2)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlants(player.getPlants() + 2);
        player.setMcIncome(player.getMcIncome() + 2);
        player.setPlantsIncome(player.getPlantsIncome() + 3);

        return null;
    }

    @Override
    public OceanRequirement getOceanRequirement() {
        return OceanRequirement.builder().minValue(6).maxValue(Constants.MAX_OCEANS).build();
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public int getPrice() {
        return 17;
    }
}
