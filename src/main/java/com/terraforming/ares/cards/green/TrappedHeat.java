package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.TerraformingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class TrappedHeat implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public TrappedHeat(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Trapped Heat")
                .description("Requires red temperature or warmer. Flip an ocean tile. During the production phase, this produces 2 heat.")
                .incomes(List.of(Gain.of(GainType.HEAT, 2)))
                .bonuses(List.of(Gain.of(GainType.OCEAN, 1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();

        Player player = marsContext.getPlayer();
        terraformingService.revealOcean(marsContext.getGame(), player);

        player.setHeatIncome(player.getHeatIncome() + 2);

        return null;
    }

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.RED, ParameterColor.YELLOW, ParameterColor.WHITE);
    }

    @Override
    public int getPrice() {
        return 20;
    }
}
