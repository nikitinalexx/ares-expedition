package com.alex.nikitin.terraformingm.services;

import com.alex.nikitin.terraformingm.factories.GameFactory;
import com.alex.nikitin.terraformingm.factories.PlanetFactory;
import com.alex.nikitin.terraformingm.model.Game;
import com.alex.nikitin.terraformingm.model.GameParameters;
import com.alex.nikitin.terraformingm.model.Planet;
import com.alex.nikitin.terraformingm.repositories.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class GameService {
    private final GameFactory gameFactory;

    private final GameRepository gameRepository;

    public void startNewGame(GameParameters gameParameters) {
        Game game = gameFactory.createGame(gameParameters);

        gameRepository.saveGame(game);
    }

}
