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
public class BeamFromThoriumAsteroid implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BeamFromThoriumAsteroid(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Beam from a Thorium Asteroid")
                .description("Requires a Jupiter tag. During the production phase, this produces 1 plant and 3 heat.")
                .incomes(List.of(
                        Gain.of(GainType.PLANT, 1),
                        Gain.of(GainType.HEAT, 3)
                ))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setPlants(player.getPlants() + 1);
        player.setHeat(player.getHeat() + 3);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlantsIncome(player.getPlantsIncome() + 1);
        player.setHeatIncome(player.getHeatIncome() + 3);

        return null;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.JUPITER);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.JUPITER, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 23;
    }
}
