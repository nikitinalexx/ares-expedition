package com.terraforming.ares.repositories.caching;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.GameUpdateResult;

import java.util.function.Function;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
public interface CachingGameRepository {

    long newGame(MarsGame game);

    MarsGame getGameById(long id);

    long getGameIdByPlayerUuid(String playerUuid);

    MarsGame getGameByPlayerUuid(String playerUuid);

    <T> GameUpdateResult<T> updateMarsGame(long id, Function<MarsGame, String> stateChecker, Function<MarsGame, T> updater);

    int evictGameCache();

}
