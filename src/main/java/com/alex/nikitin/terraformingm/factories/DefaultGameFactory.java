package com.alex.nikitin.terraformingm.factories;

import com.alex.nikitin.terraformingm.services.mars.MarsGameService;
import com.alex.nikitin.terraformingm.model.Game;
import com.alex.nikitin.terraformingm.model.GameParameters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class DefaultGameFactory implements GameFactory {
    private final MarsGameService marsGameService;

    @Override
    public Game createGame(GameParameters gameParameters) {
        return marsGameService.createNewGame(gameParameters);
    }

}
