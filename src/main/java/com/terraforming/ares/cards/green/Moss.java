package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.OceanRequirement;
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
public class Moss implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Moss(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Moss")
                .description("Requires 3 ocean tiles to be flipped and for you to spend 1 plant. During the production phase, this produces 1 plant.")
                .incomes(List.of(Gain.of(GainType.PLANT, 1)))
                .bonuses(List.of(Gain.of(GainType.PLANT, -1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlantsIncome(player.getPlantsIncome() + 1);
        player.setPlants(player.getPlants() - 1);

        return null;
    }

    @Override
    public OceanRequirement getOceanRequirement() {
        return OceanRequirement.builder().minValue(3).maxValue(Constants.MAX_OCEANS).build();
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 3;
    }
}
