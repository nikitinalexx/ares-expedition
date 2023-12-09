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
public class Potatoes implements ExperimentExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Potatoes(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Potatoes")
                .description("Spend 2 plants. During the production phase, this produces 2 MC.")
                .incomes(List.of(Gain.of(GainType.MC, 2)))
                .bonuses(List.of(Gain.of(GainType.PLANT, -2)))
                .cardAction(CardAction.POTATOES)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setMc(player.getMc() + 2);
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

        player.setMcIncome(player.getMcIncome() + 2);

        return null;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        final Player player = marsContext.getPlayer();

        player.setPlants(player.getPlants() - 2);
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 2;
    }
}
