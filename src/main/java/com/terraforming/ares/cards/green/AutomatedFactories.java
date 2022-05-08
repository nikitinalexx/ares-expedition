package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AutomatedFactories implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setCardIncome(player.getCardIncome() + 1);
        player.setCanBuildInFirstPhase(player.getCanBuildInFirstPhase() + 1);
        player.setBuiltAutomatedFactoriesLastTurn(true);

        return null;
    }

    @Override
    public String description() {
        return "You may play a green card from your hand that has a printed cost of 9 MC or less without paying its MC cost." +
                "During the production phase, draw a card.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 18;
    }
}
