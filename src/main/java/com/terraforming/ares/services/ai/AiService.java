package com.terraforming.ares.services.ai;

import com.terraforming.ares.dto.ActionsDto;
import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.StateContextProvider;
import com.terraforming.ares.services.TurnTypeService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnProcessor;
import com.terraforming.ares.states.Action;
import com.terraforming.ares.states.State;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Service
public class AiService {
    private final TurnTypeService turnTypeService;
    private final StateFactory stateFactory;
    private final StateContextProvider stateContextProvider;
    private final GameService gameService;

    private final Map<TurnType, AiTurnProcessor> turnProcessors;

    public AiService(List<AiTurnProcessor> turnProcessor,
                     TurnTypeService turnTypeService,
                     StateFactory stateFactory,
                     StateContextProvider stateContextProvider,
                     GameService gameService) {
        this.turnTypeService = turnTypeService;
        this.stateFactory = stateFactory;
        this.stateContextProvider = stateContextProvider;
        this.gameService = gameService;

        turnProcessors = turnProcessor.stream().collect(Collectors.toMap(
                AiTurnProcessor::getType, Function.identity()
        ));
    }

    public void makeAiTurns(MarsGame game) {
        game.getPlayerUuidToPlayer().values()
                .stream()
                .filter(Player::isAi)
                .forEach(player -> processAiTurn(game, player));
    }

    public boolean waitingAiTurns(MarsGame game) {
        if (game.getStateType() == StateType.GAME_END) {
            return false;
        }

        return !game.getPlayerUuidToPlayer().values()
                .stream()
                .filter(Player::isAi)
                .allMatch(player -> {
                    State currentState = stateFactory.getCurrentState(game);
                            return player.getNextTurn() != null && turnTypeService.isTerminal(player.getNextTurn().getType(), game) && !player.getNextTurn().expectedAsNextTurn()
                                    || player.getNextTurn() == null && currentState.getPossibleTurns(stateContextProvider.createStateContext(player.getUuid())).isEmpty();
                        }
                );
    }

    private void processAiTurn(MarsGame game, Player player) {
        ActionsDto nextActions = gameService.getNextActions(player.getUuid());
        String nextAction = nextActions.getPlayersToNextActions().get(player.getUuid());

        if (Action.WAIT.name().equals(nextAction)) {
            return;
        }

        List<TurnType> possibleTurns = gameService.getPossibleTurns(player.getUuid());


        TurnType turnToProcess = getTurnToProcess(possibleTurns, player);

        if (turnProcessors.containsKey(turnToProcess)) {
            boolean processed = turnProcessors.get(turnToProcess).processTurn(game, player);

            if (!processed) {
                turnProcessors.get(TurnType.SKIP_TURN).processTurn(game, player);
            }
        } else if (turnToProcess == TurnType.GAME_END) {
            System.out.println("GAME_END");
        } else {
            throw new IllegalArgumentException("Could not process the turn " + turnToProcess);
        }
    }

    private TurnType getTurnToProcess(List<TurnType> possibleTurns, Player player) {
        if (possibleTurns.contains(TurnType.PLANT_FOREST)) {
            return TurnType.PLANT_FOREST;
        }

        if (possibleTurns.contains(TurnType.INCREASE_TEMPERATURE)) {
            return TurnType.INCREASE_TEMPERATURE;
        }

        if (possibleTurns.contains(TurnType.BUILD_GREEN_PROJECT)) {
            return TurnType.BUILD_GREEN_PROJECT;
        }

        if (possibleTurns.contains(TurnType.BUILD_BLUE_RED_PROJECT)) {
            return TurnType.BUILD_BLUE_RED_PROJECT;
        }

        if (possibleTurns.contains(TurnType.UNMI_RT) && player.getMc() >= 6) {
            return TurnType.UNMI_RT;
        }

        return possibleTurns.get(0);
    }

}
