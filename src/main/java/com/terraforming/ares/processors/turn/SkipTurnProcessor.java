package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.SkipTurn;
import com.terraforming.ares.model.turn.TurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class SkipTurnProcessor implements TurnProcessor<SkipTurn> {

    @Override
    public TurnType getType() {
        return TurnType.SKIP_TURN;
    }

    @Override
    public TurnResponse processTurn(SkipTurn turn, MarsGame game) {
        int currentPhase = game.getCurrentPhase();
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        if (currentPhase == 1) {
            player.setCanBuildInFirstPhase(0);
        } else if (currentPhase == Constants.BUILD_BLUE_RED_PROJECTS_PHASE
                || currentPhase == Constants.PERFORM_BLUE_ACTION_PHASE
                || currentPhase == Constants.PICK_CORPORATIONS_PHASE) {
            player.setActionsInSecondPhase(0);
            player.setPickedCardInSecondPhase(true);
            player.setCanBuildInFirstPhase(0);
        }

        player.setBuiltSpecialDesignLastTurn(false);
        player.setBuiltWorkCrewsLastTurn(false);
        player.setCanBuildAnotherGreenWith9Discount(false);
        player.setAssortedEnterprisesDiscount(false);
        player.setSelfReplicatingDiscount(false);
        player.setMayNiDiscount(false);
        player.setAssortedEnterprisesGreenAvailable(false);
        player.setHasUnmiAction(false);
        player.setDidUnmiAction(false);

        return null;
    }
}
