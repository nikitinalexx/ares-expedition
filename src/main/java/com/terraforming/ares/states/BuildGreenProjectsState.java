package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.StateTransitionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class BuildGreenProjectsState extends AbstractState {

    public BuildGreenProjectsState(MarsContext context, StateTransitionService stateTransitionService) {
        super(context, stateTransitionService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        final MarsGame marsGame = context.getGame();
        Player player = marsGame.getPlayerByUuid(stateContext.getPlayerUuid());

        if (player.getNextTurn() != null && stateContext.getTurnTypeService().isIntermediate(player.getNextTurn().getType())) {
            return List.of(player.getNextTurn().getType());
        } else if (player.getNextTurn() != null) {
            return Collections.emptyList();
        } else if (!player.canBuildGreen()) {
            if (player.isUnmiCorporation() && player.isHasUnmiAction()) {
                List<TurnType> actions = new ArrayList<>(List.of(TurnType.UNMI_RT, TurnType.SELL_CARDS, TurnType.SKIP_TURN));
                if (isSellVpTurnAvailable()) {
                    actions.add(TurnType.SELL_VP);
                }
                return actions;
            }
            return List.of();
        } else {
            List<TurnType> actions = new ArrayList<>(List.of(
                    TurnType.BUILD_GREEN_PROJECT,
                    TurnType.SELL_CARDS
            ));
            if (isSellVpTurnAvailable()) {
                actions.add(TurnType.SELL_VP);
            }

            actions.add(TurnType.SKIP_TURN);

            return actions;
        }
    }

    @Override
    public void updateState() {
        final MarsGame marsGame = context.getGame();
        if (marsGame.getPlayerUuidToPlayer().values().stream().allMatch(
                player -> player.cantBuildAnything()
                        && player.getNextTurn() == null
                        && !player.isHasUnmiAction()
        )) {
            stateTransitionService.performStateTransferFromPhase(marsGame, 2);
        }
    }

}
