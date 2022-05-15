package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.CollectIncomeTurn;
import com.terraforming.ares.model.turn.TurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class CollectIncomeTurnProcessor implements TurnProcessor<CollectIncomeTurn> {

    @Override
    public TurnType getType() {
        return TurnType.COLLECT_INCOME;
    }

    @Override
    public TurnResponse processTurn(CollectIncomeTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        player.setMc(player.getMc() + player.getMcIncome() + player.getTerraformingRating() + (player.getChosenPhase() == 4 ? 4 : 0));
        player.setHeat(player.getHeat() + player.getHeatIncome());
        player.setPlants(player.getPlants() + player.getPlantsIncome());

        List<Integer> cards = game.dealCards(player.getCardIncome());

        for (Integer card : cards) {
            player.getHand().addCard(card);
        }

        return null;
    }
}
