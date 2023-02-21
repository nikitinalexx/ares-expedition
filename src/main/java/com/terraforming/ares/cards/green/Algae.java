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

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Algae implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Algae(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Algae")
                .description("Requires 5 ocean tiles to be flipped. During the production phase this produces 2 plants.")
                .incomes(List.of(Gain.of(GainType.PLANT, 2)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setPlants(player.getPlants() + 2);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlantsIncome(player.getPlantsIncome() + 2);

        return null;
    }

    @Override
    public OceanRequirement getOceanRequirement() {
        return OceanRequirement.builder().minValue(5).maxValue(Constants.MAX_OCEANS).build();
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 9;
    }
}
