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
public class FoodFactory implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public FoodFactory(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Food Factory")
                .description("Requires you to spend 2 plants. During the production phase, this produces 4 MC.")
                .incomes(List.of(Gain.of(GainType.MC, 4)))
                .bonuses(List.of(Gain.of(GainType.PLANT, -2)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + 4);
        player.setPlants(player.getPlants() - 2);

        return null;
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
