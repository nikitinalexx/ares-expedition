package com.terraforming.ares.repositories.caching;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.GameUpdateResult;
import com.terraforming.ares.repositories.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class CachingGameRepositoryImpl implements CachingGameRepository {
    private final GameRepository gameRepository;

    private Map<Long, MarsGame> cache = new ConcurrentHashMap<>();
    //TODO eviction
    private Map<String, Long> playerToGameIdCache = new ConcurrentHashMap<>();

    @Override
    public long newGame(MarsGame game) {
        long id = gameRepository.save(game);
        cache.put(id, game);
        for (String playerUuid : game.getPlayerUuidToPlayer().keySet()) {
            playerToGameIdCache.put(playerUuid, id);
        }
        return id;
    }

    @Override
    public MarsGame getGameById(long id) {
        return cache.computeIfAbsent(id, gameRepository::getGameById);
    }

    @Override
    public long getGameIdByPlayerUuid(String playerUuid) {
        return playerToGameIdCache.computeIfAbsent(playerUuid, key -> gameRepository.getGameIdByPlayerUuid(playerUuid));
    }

    @Override
    public MarsGame getGameByPlayerUuid(String playerUuid) {
        long gameId = getGameIdByPlayerUuid(playerUuid);
        return getGameById(gameId);
    }

    @Override
    public <T> GameUpdateResult<T> updateMarsGame(long id, Function<MarsGame, String> stateChecker, Function<MarsGame, T> updater) {
        GameUpdateResult.GameUpdateResultBuilder<T> resultBuilder = GameUpdateResult.<T>builder();

        cache.compute(id, (key, game) -> {
            if (game == null) {
                game = getGameById(key);
            }

            String validationError = stateChecker.apply(game);

            resultBuilder.error(stateChecker.apply(game));

            if (validationError == null) {
                T updateResult = updater.apply(game);
                resultBuilder.result(updateResult);

                game.iterateUpdateCounter();
                if (game.timeToSave()) {
                    gameRepository.save(game);
                }
            }

            return game;
        });

        return resultBuilder.build();
    }

}
