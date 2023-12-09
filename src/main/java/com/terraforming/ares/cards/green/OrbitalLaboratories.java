package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
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
public class OrbitalLaboratories implements ExperimentExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public OrbitalLaboratories(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Orbital Laboratories")
                .description("During the production phase, this produces 2 plants. Gain 1 plant.")
                .bonuses(List.of(Gain.of(GainType.PLANT, 1)))
                .incomes(List.of(Gain.of(GainType.PLANT, 2)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setPlants(player.getPlants() + 2);
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
        player.setPlants(player.getPlants() + 1);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.PLANT, Tag.SPACE);
    }

    @Override
    public int getPrice() {
        return 18;
    }

}
