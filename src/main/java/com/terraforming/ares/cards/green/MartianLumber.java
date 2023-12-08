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
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class MartianLumber implements ExperimentExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MartianLumber(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Martian Lumber")
                .description("When playing a Plant tag, Steel may be used as 2 MC each.")
                .incomes(List.of(Gain.of(GainType.PLANT, 1)))
                .cardAction(CardAction.MARTIAN_LUMBER)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setPlants(player.getPlants() + 1);
    }

    @Override
    public boolean canPayAgain() {
        return true;
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public int getPrice() {
        return 6;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();

        player.setPlantsIncome(player.getPlantsIncome() + 1);

        return null;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.MARTIAN_LUMBER);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.PLANT);
    }

}
