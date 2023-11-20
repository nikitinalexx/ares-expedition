package com.terraforming.ares.services;

import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.Turn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.processors.turn.TurnProcessor;
import com.terraforming.ares.repositories.caching.CachingGameRepository;
import com.terraforming.ares.repositories.crudRepositories.CrisisRecordEntityRepository;
import com.terraforming.ares.repositories.crudRepositories.GameEntityRepository;
import com.terraforming.ares.repositories.crudRepositories.PlayerEntityRepository;
import com.terraforming.ares.repositories.crudRepositories.SoloRecordEntityRepository;
import com.terraforming.ares.services.ai.AiService;
import com.terraforming.ares.services.ai.DeepNetwork;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class GameProcessorService extends BaseProcessorService {
    private final CachingGameRepository gameRepository;
    private final StateFactory stateFactory;
    private final AiService aiService;
    private final DeepNetwork deepNetwork;
    private final Queue<Long> gamesToProcess = new ArrayBlockingQueue<>(3200);
    private final SoloRecordEntityRepository soloRecordEntityRepository;
    private final CrisisRecordEntityRepository crisisRecordEntityRepository;
    private final GameEntityRepository gameEntityRepository;
    private final PlayerEntityRepository playerEntityRepository;

    public GameProcessorService(List<TurnProcessor<?>> turnProcessor,
                                CachingGameRepository gameRepository,
                                StateContextProvider stateContextProvider,
                                TurnTypeService turnTypeService,
                                StateFactory stateFactory,
                                AiService aiService,
                                SoloRecordEntityRepository soloRecordEntityRepository,
                                CrisisRecordEntityRepository crisisRecordEntityRepository,
                                GameEntityRepository gameEntityRepository,
                                PlayerEntityRepository playerEntityRepository) {
                                AiService aiService,
                                DeepNetwork deepNetwork) {
        super(
                turnTypeService,
                stateFactory,
                stateContextProvider,
                turnProcessor
        );
        this.gameRepository = gameRepository;
        this.stateFactory = stateFactory;
        this.aiService = aiService;
        this.soloRecordEntityRepository = soloRecordEntityRepository;
        this.crisisRecordEntityRepository = crisisRecordEntityRepository;
        this.gameEntityRepository = gameEntityRepository;
        this.playerEntityRepository = playerEntityRepository;
        this.deepNetwork = deepNetwork;
    }

    @Scheduled(fixedRate = 20)
    public void asyncUpdate() {
        while (!gamesToProcess.isEmpty()) {
            Long gameId = gamesToProcess.poll();

            gameRepository.updateMarsGame(gameId, game -> null, game -> {
                while (aiService.waitingAiTurns(game)) {
                    aiService.makeAiTurns(game);
                }

                while (processFinalTurns(game)) {
                    stateFactory.getCurrentState(game).updateState();
                }

                if (aiService.waitingAiTurns(game)) {
                    registerAsyncGameUpdate(gameId);
                }

                if (Constants.LOG_NET_COMPARISON && game.getStateType() == StateType.GAME_END) {
                    game.getPlayerUuidToPlayer().values().stream().filter(Player::isSecondBot).findFirst().ifPresent(
                            player -> System.out.println("Computer prediction after the game end " + deepNetwork.testState(game, player))
                    );
                }

                return null;
            });
        }
    }

    @Transactional
    @Scheduled(fixedRate = 1800000)
    public void clearMemory() {
        soloRecordEntityRepository.clearSoloRecordMemory();
        crisisRecordEntityRepository.clearCrisisRecordMemory();
        playerEntityRepository.clearPlayerMemory();
        gameEntityRepository.clearGameMemory();
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

    private Function<MarsGame, TurnResponse> getAsyncGameUpdate(Turn turn, String playerUuid) {
        return game -> {
            Player player = game.getPlayerByUuid(playerUuid);
            if (player.getNextTurn() != null && player.getNextTurn().expectedAsNextTurn()) {
                player.removeNextTurn();
            }
            player.addFirstTurn(turn);
            registerAsyncGameUpdate(game.getId());
            return null;
        };
    }

    private Function<MarsGame, TurnResponse> getSyncGameUpdate(Turn turn) {
        return game -> {
            Player player = game.getPlayerByUuid(turn.getPlayerUuid());
            if (player.getNextTurn() != null && player.getNextTurn().expectedAsNextTurn() && player.getNextTurn().getType() == turn.getType()) {
                player.removeNextTurn();
            }

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
                        .stream().filter(p -> !p.getUuid().equals(turn.getPlayerUuid()))
                        .filter(p -> p.getNextTurn() != null && p.getNextTurn().getType() == TurnType.SKIP_TURN)
                        .forEach(Player::removeNextTurn);
            }

            registerAsyncGameUpdate(game.getId());

            return turnResponse;
        };
    }

    public void registerAsyncGameUpdate(long gameId) {
        gamesToProcess.add(gameId);
    }

}
