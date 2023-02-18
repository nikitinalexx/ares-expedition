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
public class BiomassCombustors implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BiomassCombustors(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Biomass Combustors")
                .description("Requires you to spend 2 plants. During the production phase, this produces 5 heat.")
                .incomes(List.of(Gain.of(GainType.HEAT, 5)))
                .bonuses(List.of(Gain.of(GainType.PLANT, -2)))
                .cardAction(CardAction.BIOMASS_COMBUSTORS)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setHeat(player.getHeat() + 5);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setHeatIncome(player.getHeatIncome() + 5);

        return null;
    }

    @Override
    public void postProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
        player.setPlants(player.getPlants() - 2);
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
