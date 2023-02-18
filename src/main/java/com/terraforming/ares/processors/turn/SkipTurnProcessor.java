package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.SkipTurn;
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
public class SkipTurnProcessor implements TurnProcessor<SkipTurn> {
    private final CardService cardService;

    @Override
    public TurnType getType() {
        return TurnType.SKIP_TURN;
    }

    @Override
    public TurnResponse processTurn(SkipTurn turn, MarsGame game) {
        int currentPhase = game.getCurrentPhase();
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        if (currentPhase == Constants.BUILD_BLUE_RED_PROJECTS_PHASE && !player.isGotBonusInSecondPhase() && player.getChosenPhase() == 2) {
            if (player.hasPhaseUpgrade(Constants.PHASE_2_NO_UPGRADE)) {
                List<Integer> cards = cardService.dealCards(game, 1);

                for (Integer card : cards) {
                    player.getHand().addCard(card);
                }
            }

            if (player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_MC)) {
                player.setMc(player.getMc() + 6);
            }
        }

        if (currentPhase == 1) {
            player.setCanBuildInFirstPhase(0);
        } else if (currentPhase == Constants.BUILD_BLUE_RED_PROJECTS_PHASE
                || currentPhase == Constants.PERFORM_BLUE_ACTION_PHASE
                || currentPhase == Constants.PICK_CORPORATIONS_PHASE) {
            player.setActionsInSecondPhase(0);
            player.setGotBonusInSecondPhase(true);
            player.setCanBuildInFirstPhase(0);
        }

        player.setBuiltSpecialDesignLastTurn(false);
        player.setBuiltWorkCrewsLastTurn(false);
        player.setCanBuildAnotherGreenWith9Discount(false);
        player.setCanBuildAnotherGreenWithPrice12(false);
        player.setAssortedEnterprisesDiscount(false);
        player.setSelfReplicatingDiscount(false);
        player.setMayNiDiscount(false);
        player.setAssortedEnterprisesGreenAvailable(false);
        player.setHasUnmiAction(false);
        player.setDidUnmiAction(false);

        return null;
    }
}
