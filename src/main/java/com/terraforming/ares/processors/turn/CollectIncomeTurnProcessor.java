package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.CollectIncomeTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
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
    private final CardService cardService;

    @Override
    public TurnType getType() {
        return TurnType.COLLECT_INCOME;
    }

    @Override
    public TurnResponse processTurn(CollectIncomeTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        int extraMcForPhasePick = 0;

        if (player.getChosenPhase() == 4) {
            if (player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_EXTRA_MC)) {
                extraMcForPhasePick = 7;
            } else if (player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_DOUBLE_PRODUCE)) {
                extraMcForPhasePick = 1;
            } else {
                extraMcForPhasePick = 4;
            }
        }

        player.setMc(
                player.getMc() + player.getMcIncome()
                        + player.getTerraformingRating()
                        + extraMcForPhasePick
        );
        player.setHeat(player.getHeat() + player.getHeatIncome());
        player.setPlants(player.getPlants() + player.getPlantsIncome());

        List<Integer> cards = cardService.dealCards(game, player.getCardIncome());

        for (Integer card : cards) {
            player.getHand().addCard(card);
        }

        if (turn.getDoubleCollectCardId() != null) {
            cardService.getCard(turn.getDoubleCollectCardId()).payAgain(game, cardService, player);
        }

        return null;
    }
}
