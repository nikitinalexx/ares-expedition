package com.terraforming.ares.services;

import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.logic.TurnProcessor;
import com.terraforming.ares.model.turn.Turn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.repositories.GameRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.SynchronousQueue;
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
    @SuppressWarnings("unchecked")
    public void process() {
        if (gamesToProcess.isEmpty()) {
            return;
        }

        Long gameId = gamesToProcess.poll();

        gameRepository.updateMarsGame(gameId, game -> null, game -> {
            if (!game.getPlayerContexts().values().stream().allMatch(player -> player.getNextTurn() != null)) {
                return;
            }

            game.getPlayerContexts().forEach((key, value) -> {
                Turn nextTurn = value.getNextTurn();

                TurnProcessor<Turn> turnProcessor = (TurnProcessor<Turn>) turnProcessors.get(nextTurn.getType());

                turnProcessor.processTurn(nextTurn, game);

                value.setNextTurn(null);
            });

            stateFactory.getCurrentState(game).updateState();
        });
    }

    public void registerGameUpdate(long gameId) {
        gamesToProcess.add(gameId);
    }

}
