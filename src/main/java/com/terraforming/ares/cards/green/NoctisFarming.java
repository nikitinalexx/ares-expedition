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
public class NoctisFarming implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public NoctisFarming(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Noctis Farming")
                .description("Requires red temperature or warmer. Gain 2 plants. During the production phase, this produces 1 plant.")
                .incomes(List.of(Gain.of(GainType.PLANT, 1)))
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
        player.setPlantsIncome(player.getPlantsIncome() + 1);

        return null;
    }

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.RED, ParameterColor.YELLOW, ParameterColor.WHITE);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 13;
    }
}
