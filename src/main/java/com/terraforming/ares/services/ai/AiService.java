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
import com.terraforming.ares.services.ai.turnProcessors.AiFirstPhaseActionProcessor;
import com.terraforming.ares.services.ai.turnProcessors.AiSecondPhaseActionProcessor;
import com.terraforming.ares.services.ai.turnProcessors.AiThirdPhaseActionProcessor;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnProcessor;
import com.terraforming.ares.services.policyai.GameArenaContext;
import com.terraforming.ares.states.Action;
import com.terraforming.ares.states.State;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
    private final AiFirstPhaseActionProcessor aiFirstPhaseActionProcessor;
    private final AiSecondPhaseActionProcessor aiSecondPhaseActionProcessor;
    private final AiThirdPhaseActionProcessor aiThirdPhaseActionProcessor;

    private final Map<TurnType, AiTurnProcessor> turnProcessors;

    public AiService(List<AiTurnProcessor> turnProcessor,
                     TurnTypeService turnTypeService,
                     StateFactory stateFactory,
                     StateContextProvider stateContextProvider,
                     GameService gameService, AiFirstPhaseActionProcessor aiFirstPhaseActionProcessor, AiSecondPhaseActionProcessor aiSecondPhaseActionProcessor, AiThirdPhaseActionProcessor aiThirdPhaseActionProcessor) {
        this.turnTypeService = turnTypeService;
        this.stateFactory = stateFactory;
        this.stateContextProvider = stateContextProvider;
        this.gameService = gameService;

        turnProcessors = turnProcessor.stream().collect(Collectors.toMap(
                AiTurnProcessor::getType, Function.identity()
        ));
        this.aiFirstPhaseActionProcessor = aiFirstPhaseActionProcessor;
        this.aiSecondPhaseActionProcessor = aiSecondPhaseActionProcessor;
        this.aiThirdPhaseActionProcessor = aiThirdPhaseActionProcessor;
    }

    public void makeAiTurns(MarsGame game) {
        List<Player> filteredPlayers = game.getPlayerUuidToPlayer().values()
                .stream()
                .filter(player -> player.isComputer() || GameArenaContext.isSimulation())
                .collect(Collectors.toList());
        Collections.shuffle(filteredPlayers);
        for (Player filteredPlayer : filteredPlayers) {
            processAiTurn(game, filteredPlayer);
        }
    }

    public boolean waitingAiTurns(MarsGame game) {
        if (game.getStateType() == StateType.GAME_END) {
            return false;
        }

        return !game.getPlayerUuidToPlayer().values()
                .stream()
                .filter(player -> player.isComputer() || GameArenaContext.isSimulation())
                .allMatch(player -> {
                            State currentState = stateFactory.getCurrentState(game);
                            boolean alreadyMadeATurn = player.getNextTurn() != null && turnTypeService.isTerminal(player.getNextTurn().getType(), game) && !player.getNextTurn().expectedAsNextTurn();
                            boolean noTurnsInCurrentPhase = player.getNextTurn() == null && currentState.getPossibleTurns(stateContextProvider.createStateContext(player.getUuid())).isEmpty();

                            return alreadyMadeATurn || noTurnsInCurrentPhase;
                        }
                );
    }

    private void processAiTurn(MarsGame game, Player player) {
        ActionsDto nextActions = gameService.getNextActions(game, player.getUuid());
        String nextAction = nextActions.getPlayersToNextActions().get(player.getUuid());

        if (Action.WAIT.name().equals(nextAction)) {
            return;
        }

        List<TurnType> possibleTurns = gameService.getPossibleTurns(game, player.getUuid());

        if (possibleTurns.size() == 1 && possibleTurns.getFirst() == TurnType.DISCARD_CARDS) {
            turnProcessors.get(possibleTurns.getFirst()).processTurn(game, player);
        } else if (game.getStateType() == StateType.PERFORM_BLUE_ACTION) {
            aiThirdPhaseActionProcessor.processTurn(possibleTurns, game, player);
        } else if (game.getStateType() == StateType.BUILD_BLUE_RED_PROJECTS || possibleTurns.contains(TurnType.BUILD_BLUE_RED_PROJECT)) {
            aiSecondPhaseActionProcessor.processTurn(possibleTurns, game, player);
        } else if (game.getStateType() == StateType.BUILD_GREEN_PROJECTS) {
            aiFirstPhaseActionProcessor.processTurn(possibleTurns, game, player);
        } else {
            TurnType turnToProcess = getTurnToProcess(possibleTurns);

            if (turnProcessors.containsKey(turnToProcess)) {
                turnProcessors.get(turnToProcess).processTurn(game, player);
            } else if (turnToProcess == TurnType.BUILD_GREEN_PROJECT) {
                aiFirstPhaseActionProcessor.processTurn(possibleTurns, game, player);
            } else {
                throw new IllegalArgumentException("Could not process the turn " + turnToProcess);
            }
        }
    }

    private TurnType getTurnToProcess(List<TurnType> possibleTurns) {
        if (possibleTurns.contains(TurnType.MULLIGAN)) {
            return TurnType.MULLIGAN;
        }

        if (possibleTurns.contains(TurnType.BUILD_GREEN_PROJECT)) {
            return TurnType.BUILD_GREEN_PROJECT;
        }

        return possibleTurns.get(0);
    }

}