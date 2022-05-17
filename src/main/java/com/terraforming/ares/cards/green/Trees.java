package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Trees implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Trees(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Trees")
                .description("Requires yellow temperature or warmer. Gain 1 plant. During the production phase, this produces 3 plants.")
                .incomes(List.of(Gain.of(GainType.PLANT, 3)))
                .bonuses(List.of(Gain.of(GainType.PLANT, 3)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlantsIncome(player.getPlantsIncome() + 3);
        player.setPlants(player.getPlants() + 1);

        return null;
    }

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 17;
    }
}
