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
public class BuildingIndustries implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BuildingIndustries(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Building Industries")
                .description("Requires you to spend 4 heat. When you play a Building, you pay 4 MC less for it.")
                .incomes(List.of(Gain.of(GainType.STEEL, 2)))
                .cardAction(CardAction.BUILDING_INDUSTRIES)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setSteelIncome(player.getSteelIncome() + 2);
        player.setHeat(player.getHeat() - 4);

        return null;
    }

    @Override
    public int heatSpendOnBuild() {
        return 4;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 6;
    }
}
