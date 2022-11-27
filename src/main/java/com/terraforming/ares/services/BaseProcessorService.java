package com.terraforming.ares.services;

import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.Turn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.processors.turn.TurnProcessor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 26.11.2022
 */
public abstract class BaseProcessorService {
    private final TurnTypeService turnTypeService;
    private final StateFactory stateFactory;
    private final StateContextProvider stateContextProvider;
    private final Map<TurnType, TurnProcessor<?>> turnProcessors;

    protected BaseProcessorService(TurnTypeService turnTypeService,
                                   StateFactory stateFactory,
                                   StateContextProvider stateContextProvider,
                                   List<TurnProcessor<?>> turnProcessor) {
        this.turnTypeService = turnTypeService;
        this.stateFactory = stateFactory;
        this.stateContextProvider = stateContextProvider;
        this.turnProcessors = turnProcessor.stream().collect(Collectors.toMap(TurnProcessor::getType, Function.identity()));
    }

    protected boolean processFinalTurns(MarsGame game) {
        boolean allTurnsReadyAndAllTerminal = game.getPlayerUuidToPlayer()
                .values()
                .stream()
                .allMatch(player -> player.getNextTurn() != null && turnTypeService.isTerminal(player.getNextTurn().getType(), game) && !player.getNextTurn().expectedAsNextTurn()
                        || player.getNextTurn() == null && stateFactory.getCurrentState(game).getPossibleTurns(stateContextProvider.createStateContext(player.getUuid())).isEmpty()
                );

        if (!allTurnsReadyAndAllTerminal) {
            return false;
        }

        game.getPlayerUuidToPlayer().values().forEach(player -> processNextTurn(player, game));

        return true;
    }

    private void processNextTurn(Player player, MarsGame game) {
        Turn turnToProcess = player.getNextTurn();
        player.removeNextTurn();

        processTurn(turnToProcess, game);
    }

    @SuppressWarnings("unchecked")
    protected TurnResponse processTurn(Turn turn, MarsGame game) {
        if (turn == null) {
            return null;
        }

        TurnProcessor<Turn> turnProcessor = (TurnProcessor<Turn>) turnProcessors.get(turn.getType());

        return turnProcessor.processTurn(turn, game);
    }

}
