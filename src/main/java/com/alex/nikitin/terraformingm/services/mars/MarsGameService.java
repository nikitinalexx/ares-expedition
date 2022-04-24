package com.alex.nikitin.terraformingm.services.mars;

import com.alex.nikitin.terraformingm.factories.PlanetFactory;
import com.alex.nikitin.terraformingm.mars.MarsGame;
import com.alex.nikitin.terraformingm.model.Deck;
import com.alex.nikitin.terraformingm.model.GameParameters;
import com.alex.nikitin.terraformingm.model.Planet;
import com.alex.nikitin.terraformingm.services.ShuffleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class MarsGameService {

    private final MarsDeckService marsDeckService;
    private final ShuffleService shuffleService;
    private final PlanetFactory planetFactory;

    public MarsGame createNewGame(GameParameters gameParameters) {
        Deck projectsDeck = marsDeckService.createProjectsDeck();
        Deck corporationsDeck = marsDeckService.createCorporationsDeck();

        shuffleService.shuffle(projectsDeck.getCards());
        shuffleService.shuffle(corporationsDeck.getCards());

        Planet mars = planetFactory.createMars(gameParameters);

        return new MarsGame(gameParameters.getPlayersCount(), projectsDeck, corporationsDeck, mars);
    }

}
