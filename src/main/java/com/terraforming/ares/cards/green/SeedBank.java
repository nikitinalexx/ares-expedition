package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 04.12.2023
 */
@RequiredArgsConstructor
@Getter
public class SeedBank implements InfrastructureExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public SeedBank(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Seed Bank")
                .description("Requires red infrastructure or higher. During the production phase, this produces 2 plants and 3 heat.")
                .incomes(List.of(Gain.of(GainType.PLANT, 2), Gain.of(GainType.HEAT, 3)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setPlants(player.getPlants() + 2);
        player.setHeat(player.getHeat() + 3);
    }

    @Override
    public boolean canPayAgain() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlantsIncome(player.getPlantsIncome() + 2);
        player.setHeatIncome(player.getHeatIncome() + 3);

        return null;
    }

    @Override
    public List<ParameterColor> getInfrastructureRequirement() {
        return List.of(ParameterColor.R, ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 16;
    }

}
