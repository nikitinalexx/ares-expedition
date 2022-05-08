package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class TallStation implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + 3);
        player.setCanBuildInFirstPhase(player.getCanBuildInFirstPhase() + 1);
        player.setCanBuildAnotherGreenWith9Discount(true);

        return null;
    }

    @Override
    public String description() {
        return "You may play a green card from your hand that has a printed cost of 9 MC or less without payind its MC cost. During the production phase, this produces 3 МС.";
    }

    @Override
    public int getPrice() {
        return 16;
    }
}
