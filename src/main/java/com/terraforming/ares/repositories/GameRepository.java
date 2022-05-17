package com.terraforming.ares.repositories;

import com.terraforming.ares.mars.MarsGame;

/**
 * Created by oleksii.nikitin
 * Creation date 17.05.2022
 */
public interface GameRepository {

    long save(MarsGame game);

    MarsGame getGameById(long id);

    long getGameIdByPlayerUuid(String playerUuid);
}
