package com.terraforming.ares.repositories;

import com.terraforming.ares.mars.MarsGame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class TemporaryGameRepository implements GameRepository {
    private final GameRepository realStorage;

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
    public String updateMarsGame(long id, Function<MarsGame, String> stateChecker, Consumer<MarsGame> updater) {
        ResultHolder resultHolder = new ResultHolder();

        cache.compute(id, (key, value) -> {
            if (value == null) {
                value = getGameById(key);
            }

            String errorMessage = stateChecker.apply(value);

            if (errorMessage == null) {
                updater.accept(value);
                realStorage.save(value);
                resultHolder.errorMessage = errorMessage;
            }

            return value;
        });

        return resultHolder.errorMessage;
    }

    private static class ResultHolder {
        String errorMessage;
    }

}
