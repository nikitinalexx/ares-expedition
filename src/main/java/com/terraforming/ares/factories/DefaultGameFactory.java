package com.terraforming.ares.factories;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ShuffleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class DefaultGameFactory implements GameFactory {
    private final CardService cardService;
    private final PlanetFactory planetFactory;
    private final AchievementsFactory achievementsFactory;
    private final ShuffleService shuffleService;

    @Override
    public MarsGame createMarsGame(GameParameters gameParameters) {
        Deck projectsDeck = cardService.createProjectsDeck(gameParameters.getExpansions());
        Deck corporationsDeck = cardService.createCorporationsDeck(gameParameters.getExpansions());

        Planet mars = planetFactory.createMars(gameParameters);

        List<Integer> dummyHand = null;
        if (gameParameters.isDummyHand()) {
            dummyHand = IntStream.rangeClosed(1, 5).boxed().collect(Collectors.toList());
            shuffleService.shuffle(dummyHand);
        }

        return new MarsGame(
                gameParameters.getPlayerNames(),
                Constants.DEFAULT_START_HAND_SIZE,
                projectsDeck,
                corporationsDeck,
                mars,
                gameParameters.isMulligan(),
                achievementsFactory.createAwards(Constants.ACHIEVEMENTS_SIZE),
                achievementsFactory.createMilestones(Constants.ACHIEVEMENTS_SIZE),
                gameParameters.getComputers(),
                gameParameters.getExpansions(),
                gameParameters.isDummyHand(),
                dummyHand
        );

    }

}
