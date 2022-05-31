package com.terraforming.ares.services;

import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.GameUpdateResult;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.Turn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.processors.turn.TurnProcessor;
import com.terraforming.ares.repositories.caching.CachingGameRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class GameProcessorService {
    private final CachingGameRepository gameRepository;
    private final StateFactory stateFactory;
    private final PaymentValidationService paymentValidationService;
    private final Map<TurnType, TurnProcessor<?>> turnProcessors;
    private final Queue<Long> gamesToProcess = new ArrayBlockingQueue<>(100);

    public GameProcessorService(List<TurnProcessor<?>> turnProcessor,
                                CachingGameRepository gameRepository,
                                PaymentValidationService paymentValidationService,
                                StateFactory stateFactory) {
        this.gameRepository = gameRepository;
        this.stateFactory = stateFactory;
        this.paymentValidationService = paymentValidationService;

        turnProcessors = turnProcessor.stream().collect(Collectors.toMap(
                TurnProcessor::getType, Function.identity()
        ));
    }

    @Scheduled(fixedRate = 1000)
    public void asyncUpdate() {
        if (gamesToProcess.isEmpty()) {
            return;
        }

        Long gameId = gamesToProcess.poll();

        gameRepository.updateMarsGame(gameId, game -> null, game -> {
            while (processFinalTurns(game)) {
                stateFactory.getCurrentState(game).updateState();
            }

            return null;
        });
    }

    public GameUpdateResult<TurnResponse> performTurn(long gameId,
                                                      Turn turn,
                                                      String playerUuid,
                                                      Function<MarsGame, String> stateChecker,
                                                      Predicate<MarsGame> syncTurnDecider) {
        return gameRepository.updateMarsGame(gameId, stateChecker, game -> {
            boolean isSyncTurn = syncTurnDecider.test(game);

            return (isSyncTurn
                    ? getSyncGameUpdate(turn)
                    : getAsyncGameUpdate(turn, playerUuid)
            )
                    .apply(game);
        });
    }

    public GameUpdateResult<TurnResponse> syncPlayerUpdate(long gameId, Turn turn, Function<MarsGame, String> stateChecker) {
        return gameRepository.updateMarsGame(gameId, stateChecker, getSyncGameUpdate(turn));
    }

    private Function<MarsGame, TurnResponse> getAsyncGameUpdate(Turn turn, String playerUuid) {
        return game -> {
            game.getPlayerByUuid(playerUuid).setNextTurn(turn);
            registerAsyncGameUpdate(game.getId());
            return null;
        };
    }

    private Function<MarsGame, TurnResponse> getSyncGameUpdate(Turn turn) {
        return game -> {
            boolean oxygenMaxBefore = game.getPlanet().isOxygenMax();
            boolean temperatureMaxBefore = game.getPlanet().isTemperatureMax();
            boolean oceansMaxBefore = game.getPlanet().isOceansMax();

            TurnResponse turnResponse = processTurn(turn, game);

            boolean oxygenMaxAfter = game.getPlanet().isOxygenMax();
            boolean temperatureMaxAfter = game.getPlanet().isTemperatureMax();
            boolean oceansMaxAfter = game.getPlanet().isOceansMax();

            if (game.getCurrentPhase() == 3 &&
                    (!oxygenMaxBefore && oxygenMaxAfter
                            || !temperatureMaxBefore && temperatureMaxAfter
                            || !oceansMaxBefore && oceansMaxAfter)) {
                game.getPlayerUuidToPlayer()
                        .values()
                        .stream().filter(player -> !player.getUuid().equals(turn.getPlayerUuid()))
                        .filter(player -> player.getNextTurn() != null && player.getNextTurn().getType() == TurnType.SKIP_TURN)
                        .forEach(player -> player.setNextTurn(null));
            }

            registerAsyncGameUpdate(game.getId());

            return turnResponse;
        };
    }

    private boolean processFinalTurns(MarsGame game) {
        boolean allTurnsReadyAndAllTerminal = game.getPlayerUuidToPlayer()
                .values()
                .stream()
                .allMatch(player -> player.getNextTurn() != null && player.getNextTurn().getType().isTerminal()
                        || player.getNextTurn() == null && stateFactory.getCurrentState(game).getPossibleTurns(new StateContext(player.getUuid(), paymentValidationService)).isEmpty()
                );

        if (!allTurnsReadyAndAllTerminal) {
            return false;
        }

        game.getPlayerUuidToPlayer().values().forEach(player -> processNextTurn(player, game));

        return true;
    }

    private void processNextTurn(Player player, MarsGame game) {
        Turn turnToProcess = player.getNextTurn();
        processTurn(turnToProcess, game);

        if (turnToProcess == player.getNextTurn()) {
            player.setNextTurn(null);
        }
    }

    @SuppressWarnings("unchecked")
    private TurnResponse processTurn(Turn turn, MarsGame game) {
        if (turn == null) {
            return null;
        }

        TurnProcessor<Turn> turnProcessor = (TurnProcessor<Turn>) turnProcessors.get(turn.getType());

        return turnProcessor.processTurn(turn, game);
    }

    public void registerAsyncGameUpdate(long gameId) {
        gamesToProcess.add(gameId);
    }

}
