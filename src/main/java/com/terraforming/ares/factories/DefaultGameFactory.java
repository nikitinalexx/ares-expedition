package com.terraforming.ares.factories;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.ShuffleService;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class DefaultGameFactory implements GameFactory {
    private final CardService marsDeckService;
    private final ShuffleService shuffleService;
    private final PlanetFactory planetFactory;

    @Override
    public MarsGame createMarsGame(GameParameters gameParameters) {
        Deck projectsDeck = marsDeckService.createProjectsDeck(Collections.singletonList(Expansion.BASE));
        Deck corporationsDeck = marsDeckService.createCorporationsDeck(Collections.singletonList(Expansion.BASE));

        shuffleService.shuffle(projectsDeck.getCards());
        shuffleService.shuffle(corporationsDeck.getCards());

        Planet mars = planetFactory.createMars(gameParameters);

        return new MarsGame(
                gameParameters.getPlayerNames(),
                Constants.DEFAULT_START_HAND_SIZE,
                projectsDeck,
                corporationsDeck,
                mars
        );

    }

}
