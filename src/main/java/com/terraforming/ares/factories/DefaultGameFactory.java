package com.terraforming.ares.factories;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.services.ShuffleService;
import com.terraforming.ares.services.MarsDeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class DefaultGameFactory implements GameFactory {
    private final MarsDeckService marsDeckService;
    private final ShuffleService shuffleService;
    private final PlanetFactory planetFactory;

    @Override
    public MarsGame createMarsGame(GameParameters gameParameters) {
        Deck projectsDeck = marsDeckService.createProjectsDeck();
        Deck corporationsDeck = marsDeckService.createCorporationsDeck();

        shuffleService.shuffle(projectsDeck.getCards());
        shuffleService.shuffle(corporationsDeck.getCards());

        Planet mars = planetFactory.createMars(gameParameters);

        return new MarsGame(gameParameters.getPlayersCount(), projectsDeck, corporationsDeck, mars);

    }

}
