package com.alex.nikitin.terraformingm.repositories;

import com.alex.nikitin.terraformingm.model.Game;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
public interface GameRepository {

    void saveGame(Game game);

}
