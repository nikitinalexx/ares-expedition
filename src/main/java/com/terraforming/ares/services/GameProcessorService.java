package com.terraforming.ares.services;

import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.turn.Turn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.repositories.GameRepository;
import com.terraforming.ares.turnProcessors.TurnProcessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class GameProcessorService {
    private final GameRepository gameRepository;
    private final StateFactory stateFactory;
    private final Map<TurnType, TurnProcessor<?>> turnProcessors;
    private final Queue<Long> gamesToProcess = new ArrayBlockingQueue<>(100);

    public GameProcessorService(List<TurnProcessor<?>> turnProcessor,
                                GameRepository gameRepository,
                                StateFactory stateFactory) {
        this.gameRepository = gameRepository;
        this.stateFactory = stateFactory;

        turnProcessors = turnProcessor.stream().collect(Collectors.toMap(
                TurnProcessor::getType, Function.identity()
        ));
    }


    @Scheduled(fixedRate = 1000)
    public void process() {
        if (gamesToProcess.isEmpty()) {
            return;
        }

        Long gameId = gamesToProcess.poll();

        gameRepository.updateMarsGame(gameId, game -> null, game -> {

            if (processFinalTurns(game)) {
                stateFactory.getCurrentState(game).updateState();
                return;
            }

            processIntermediateTurns(game);
        });


    }

    private void processIntermediateTurns(MarsGame game) {
        game.getPlayerContexts()
                .values()
                .stream()
                .filter(player -> player.getNextTurn() != null && !player.getNextTurn().getType().isTerminal())
                .forEach(playerContext -> processNextTurn(playerContext, game));
    }

    private boolean processFinalTurns(MarsGame game) {
        boolean allTurnsReadyAndAllTerminal = game.getPlayerContexts()
                .values()
                .stream()
                .allMatch(player -> player.getNextTurn() != null && player.getNextTurn().getType().isTerminal());

        if (!allTurnsReadyAndAllTerminal) {
            return false;
        }

        game.getPlayerContexts().values().forEach(playerContext -> processNextTurn(playerContext, game));

        return true;
    }

    @SuppressWarnings("unchecked")
    private void processNextTurn(PlayerContext playerContext, MarsGame game) {
        TurnProcessor<Turn> turnProcessor = (TurnProcessor<Turn>) turnProcessors.get(playerContext.getNextTurn().getType());

        turnProcessor.processTurn(playerContext.getNextTurn(), game);

        playerContext.setNextTurn(null);
    }

    public void registerGameUpdate(long gameId) {
        gamesToProcess.add(gameId);
    }

}
