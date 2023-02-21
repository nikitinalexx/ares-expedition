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

/**
 * Created by oleksii.nikitin
 * Creation date 17.02.2023
 */
@RequiredArgsConstructor
@Getter
public class Tourism implements DiscoveryExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Tourism(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Tourism")
                .description("Raise your TR 1 step for every your Milestone. During the production phase, this produces 2 MC.")
                .cardAction(CardAction.TOURISM)
                .incomes(List.of(Gain.of(GainType.MC, 2)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setMc(player.getMc() + 2);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        int milestonesAchieved = (int) marsContext.getGame()
                .getMilestones()
                .stream()
                .filter(milestone -> milestone.isAchieved(player)).count();

        player.setTerraformingRating(player.getTerraformingRating() + milestonesAchieved);
        player.setMcIncome(player.getMcIncome() + 2);

        return null;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 8;
    }

}
