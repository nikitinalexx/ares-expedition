package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class BuildBlueRedProjectsState extends AbstractState {

    public BuildBlueRedProjectsState(MarsGame marsGame, CardService cardService) {
        super(marsGame, cardService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        Player player = marsGame.getPlayerByUuid(stateContext.getPlayerUuid());
        if (player.getNextTurn() != null && stateContext.getTurnTypeService().isIntermediate(player.getNextTurn().getType())) {
            return List.of(player.getNextTurn().getType());
        } else if (player.getNextTurn() != null) {
            return List.of();
        } else if ((player.getActionsInSecondPhase() == 0 && player.getCanBuildInFirstPhase() == 0)) {
            if (player.isUnmiCorporation() && player.isHasUnmiAction()) {
                return List.of(TurnType.UNMI_RT, TurnType.SELL_CARDS, TurnType.SKIP_TURN);
            }
            return List.of();
        } else {
            List<TurnType> actions = new ArrayList<>(List.of(
                    TurnType.SELL_CARDS,
                    TurnType.SKIP_TURN
            ));
            if (player.getChosenPhase() == 2 && !player.isPickedCardInSecondPhase()) {
                actions.add(TurnType.PICK_EXTRA_CARD);
            }
            if (player.isAssortedEnterprisesGreenAvailable() || player.getCanBuildInFirstPhase() > 0) {
                actions.add(TurnType.BUILD_GREEN_PROJECT);
            }
            if (player.getActionsInSecondPhase() > 0) {
                actions.add(TurnType.BUILD_BLUE_RED_PROJECT);
            }
            if (player.isUnmiCorporation() && player.isHasUnmiAction()) {
                actions.add(TurnType.UNMI_RT);
            }
            return actions;
        }
    }

    @Override
    public void updateState() {
        if (marsGame.getPlayerUuidToPlayer().values().stream().allMatch(
                player -> player.getActionsInSecondPhase() == 0
                        && player.getCanBuildInFirstPhase() == 0
                        && player.getNextTurn() == null
                        && !player.isHasUnmiAction()
        )) {
            performStateTransferFromPhase(3);
        }
    }
}
