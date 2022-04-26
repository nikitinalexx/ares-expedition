package com.terraforming.ares.repositories;

import com.terraforming.ares.mars.MarsGame;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
//TODO replace
@Service
public class InMemoryGameRepository {
    private MarsGame game;

    public long save(MarsGame game) {
        game.setId(0);
        this.game = game;
        return 0;
    }

    public MarsGame getGameById(long id) {
        return game;
    }

    public long getGameIdByPlayerUuid(String playerUuid) {
        return 0;
    }

    public MarsGame getGameByPlayerUuid(String playerUuid) {
        return game;
    }

    public String updateMarsGame(long id, Function<MarsGame, String> stateChecker, Consumer<MarsGame> updater) {
        return null;
    }

}
