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
 * Creation date 17.02.2023
 */
@RequiredArgsConstructor
@Getter
public class AwardWinningReflectorMaterial implements DiscoveryExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AwardWinningReflectorMaterial(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Award Winning Reflector Material")
                .description("If you have a Milestone, gain 4 heat. During the production phase this produces 3 heat.")
                .cardAction(CardAction.AWARD_WINNING_REFLECTOR)
                .incomes(List.of(Gain.of(GainType.HEAT, 3)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setHeat(player.getHeat() + 3);
    }

    @Override
    public boolean canPayAgain() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();

        player.setHeatIncome(player.getHeatIncome() + 3);

        return null;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        final Player player = marsContext.getPlayer();
        if (marsContext.getGame().getMilestones().stream().anyMatch(milestone -> milestone.isAchieved(player))) {
            player.setHeat(player.getHeat() + 4);
        }
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
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 14;
    }

}
