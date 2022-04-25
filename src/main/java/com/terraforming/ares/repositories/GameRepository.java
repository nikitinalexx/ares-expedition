package com.terraforming.ares.repositories;

import com.terraforming.ares.mars.MarsGame;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
public interface GameRepository {

    long save(MarsGame game);

    MarsGame getGameById(long id);

    long getGameIdByPlayerUuid(String playerUuid);

    MarsGame getGameByPlayerUuid(String playerUuid);

    String updateMarsGame(long id, Function<MarsGame, String> stateChecker, Consumer<MarsGame> updater);

}
