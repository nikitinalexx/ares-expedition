package com.terraforming.ares.services.ai;

import com.terraforming.ares.dto.ActionsDto;
import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.StateContextProvider;
import com.terraforming.ares.services.TurnTypeService;
import com.terraforming.ares.services.ai.turnProcessors.AiSecondPhaseActionProcessor;
import com.terraforming.ares.services.ai.turnProcessors.AiThirdPhaseActionProcessor;
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
    private final AiSecondPhaseActionProcessor aiSecondPhaseActionProcessor;
    private final AiThirdPhaseActionProcessor aiThirdPhaseActionProcessor;

    private final Map<TurnType, AiTurnProcessor> turnProcessors;

    public AiService(List<AiTurnProcessor> turnProcessor,
                     TurnTypeService turnTypeService,
                     StateFactory stateFactory,
                     StateContextProvider stateContextProvider,
                     GameService gameService, AiSecondPhaseActionProcessor aiSecondPhaseActionProcessor, AiThirdPhaseActionProcessor aiThirdPhaseActionProcessor) {
        this.turnTypeService = turnTypeService;
        this.stateFactory = stateFactory;
        this.stateContextProvider = stateContextProvider;
        this.gameService = gameService;

        turnProcessors = turnProcessor.stream().collect(Collectors.toMap(
                AiTurnProcessor::getType, Function.identity()
        ));
        this.aiSecondPhaseActionProcessor = aiSecondPhaseActionProcessor;
        this.aiThirdPhaseActionProcessor = aiThirdPhaseActionProcessor;
    }

    public void makeAiTurns(MarsGame game) {
        game.getPlayerUuidToPlayer().values()
                .stream()
                .filter(Player::isComputer)
                .filter(player -> !passTurnToRealPlayer(player, game))
                .forEach(player -> processAiTurn(game, player));
    }

    public boolean waitingAiTurns(MarsGame game) {
        if (game.getStateType() == StateType.GAME_END) {
            return false;
        }

        return !game.getPlayerUuidToPlayer().values()
                .stream()
                .filter(Player::isComputer)
                .allMatch(player -> {
                            State currentState = stateFactory.getCurrentState(game);
                            boolean alreadyMadeATurn = player.getNextTurn() != null && turnTypeService.isTerminal(player.getNextTurn().getType(), game) && !player.getNextTurn().expectedAsNextTurn();
                            boolean noTurnsInCurrentPhase = player.getNextTurn() == null && currentState.getPossibleTurns(stateContextProvider.createStateContext(player.getUuid())).isEmpty();
                            if (passTurnToRealPlayer(player, game)) {
                                alreadyMadeATurn = true;//pass it to real player
                            }
                            return alreadyMadeATurn || noTurnsInCurrentPhase;
                        }
                );
    }

    private boolean passTurnToRealPlayer(Player player, MarsGame game) {
        if (!player.isFirstBot() || !Constants.PLAYER_CHOOSES_CARDS_FOR_FIRST_PLAYER) {
            return false;
        }
        State currentState = stateFactory.getCurrentState(game);

        if (player.getNextTurn() == null) {
            List<TurnType> possibleTurns = currentState.getPossibleTurns(stateContextProvider.createStateContext(player.getUuid()));
            if (possibleTurns.contains(TurnType.MULLIGAN) || possibleTurns.contains(TurnType.SELL_CARDS_LAST_ROUND)) {
                return true;
            }
        } else {
            if (player.getNextTurn().getType() != TurnType.DISCARD_CARDS) {
                return false;
            }

            if (game.getCurrentPhase() != Constants.PICK_CORPORATIONS_PHASE && game.getCurrentPhase() != Constants.DRAFT_CARDS_PHASE) {
                return false;
            }

            return true;
        }

        return false;
    }

    private void processAiTurn(MarsGame game, Player player) {
        ActionsDto nextActions = gameService.getNextActions(game, player.getUuid());
        String nextAction = nextActions.getPlayersToNextActions().get(player.getUuid());

        if (Action.WAIT.name().equals(nextAction)) {
            return;
        }

        //TODO mixing stateType and possibleTurns could be made better?
        List<TurnType> possibleTurns = gameService.getPossibleTurns(game, player.getUuid());

        if (possibleTurns.size() == 1 && possibleTurns.get(0) == TurnType.DISCARD_CARDS) {
            turnProcessors.get(possibleTurns.get(0)).processTurn(game, player);
        } else if (game.getStateType() == StateType.PERFORM_BLUE_ACTION) {
            aiThirdPhaseActionProcessor.processTurn(possibleTurns, game, player);
        } else if (game.getStateType() == StateType.BUILD_BLUE_RED_PROJECTS || possibleTurns.contains(TurnType.BUILD_BLUE_RED_PROJECT)) {
            aiSecondPhaseActionProcessor.processTurn(possibleTurns, game, player);
        } else {
            TurnType turnToProcess = getTurnToProcess(possibleTurns, player);

            if (turnProcessors.containsKey(turnToProcess)) {
                turnProcessors.get(turnToProcess).processTurn(game, player);
            } else {
                throw new IllegalArgumentException("Could not process the turn " + turnToProcess);
            }
        }
    }

    private TurnType getTurnToProcess(List<TurnType> possibleTurns, Player player) {
        if (possibleTurns.contains(TurnType.MULLIGAN)) {
            return TurnType.MULLIGAN;
        }

        if (possibleTurns.contains(TurnType.UNMI_RT) && player.getMc() >= 6) {
            return TurnType.UNMI_RT;
        }

        if (possibleTurns.contains(TurnType.BUILD_GREEN_PROJECT)) {
            return TurnType.BUILD_GREEN_PROJECT;
        }

//        if (possibleTurns.contains(TurnType.BUILD_BLUE_RED_PROJECT)) {
//            return TurnType.BUILD_BLUE_RED_PROJECT;
//        }

//        if (possibleTurns.contains(TurnType.PERFORM_BLUE_ACTION)) {
//            return TurnType.PERFORM_BLUE_ACTION;
//        }

        for (int i = 0; i < possibleTurns.size(); i++) {
            if (possibleTurns.get(i) != TurnType.UNMI_RT && possibleTurns.get(i) != TurnType.SELL_CARDS) {
                return possibleTurns.get(i);
            }
        }

        throw new IllegalStateException("Unreachable");
    }

}