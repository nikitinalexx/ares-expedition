package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.StateTransitionService;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class BuildGreenProjectsState extends AbstractState {

    public BuildGreenProjectsState(MarsGame marsGame, CardService cardService, StateTransitionService stateTransitionService) {
        super(marsGame, cardService, stateTransitionService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        Player player = marsGame.getPlayerByUuid(stateContext.getPlayerUuid());

        if (player.getNextTurn() != null && stateContext.getTurnTypeService().isIntermediate(player.getNextTurn().getType())) {
            return List.of(player.getNextTurn().getType());
        } else if (player.getNextTurn() != null) {
            return Collections.emptyList();
        } else if (!player.canBuildGreen()) {
            if (player.isUnmiCorporation() && player.isHasUnmiAction()) {
                return List.of(TurnType.UNMI_RT, TurnType.SELL_CARDS, TurnType.SKIP_TURN);
            }
            return List.of();
        } else {
            return List.of(
                    TurnType.BUILD_GREEN_PROJECT,
                    TurnType.SELL_CARDS,
                    TurnType.SKIP_TURN
            );
        }
    }

    @Override
    public void updateState() {
        if (marsGame.getPlayerUuidToPlayer().values().stream().allMatch(
                player -> player.cantBuildAnything()
                        && player.getNextTurn() == null
                        && !player.isHasUnmiAction()
        )) {
            stateTransitionService.performStateTransferFromPhase(marsGame, 2);
        }
    }

}
