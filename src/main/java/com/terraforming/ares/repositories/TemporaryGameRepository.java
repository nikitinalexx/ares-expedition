package com.terraforming.ares.repositories;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.GameUpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
//TODO replace
@Service
@RequiredArgsConstructor
public class TemporaryGameRepository implements GameRepository {
    private final InMemoryGameRepository realStorage;

    private Map<Long, MarsGame> cache = new ConcurrentHashMap<>();

    @Override
    public long save(MarsGame game) {
        return realStorage.save(game);
    }

    @Override
    public MarsGame getGameById(long id) {
        return cache.computeIfAbsent(id, realStorage::getGameById);
    }

    @Override
    public long getGameIdByPlayerUuid(String playerUuid) {
        return realStorage.getGameIdByPlayerUuid(playerUuid);
    }

    @Override
    public MarsGame getGameByPlayerUuid(String playerUuid) {
        long gameId = getGameIdByPlayerUuid(playerUuid);
        return getGameById(gameId);
    }

    @Override
    public <T> GameUpdateResult<T> updateMarsGame(long id, Function<MarsGame, String> stateChecker, Function<MarsGame, T> updater) {
        GameUpdateResult.GameUpdateResultBuilder<T> resultBuilder = GameUpdateResult.<T>builder();

        cache.compute(id, (key, value) -> {
            if (value == null) {
                value = getGameById(key);
            }

            String validationError = stateChecker.apply(value);

            resultBuilder.error(stateChecker.apply(value));

            if (validationError == null) {
                T updateResult = updater.apply(value);
                resultBuilder.result(updateResult);
                realStorage.save(value);
            }

            return value;
        });

        return resultBuilder.build();
    }

}
