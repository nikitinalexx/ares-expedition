package com.terraforming.ares.factories;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ShuffleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
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
        Deck corporationsDeck = cardService.createCorporationsDeck(new HashSet<>(gameParameters.getExpansions()));
        Deck crysisDeck = cardService.createCrysisDeck(gameParameters.getExpansions(), gameParameters.getPlayerNames().size());

        Planet mars = planetFactory.createMars(gameParameters);

        List<Integer> dummyHand = new ArrayList<>();
        if (gameParameters.isDummyHand()) {
            dummyHand = IntStream.rangeClosed(1, 5).boxed().collect(Collectors.toList());
            shuffleService.shuffle(dummyHand);
        }

        final boolean isCrysisExpansion = gameParameters.getExpansions().contains(Expansion.CRYSIS);
        return new MarsGame(
                gameParameters.getPlayerNames(),
                Constants.DEFAULT_START_HAND_SIZE,
                projectsDeck,
                corporationsDeck,
                mars,
                gameParameters.isMulligan(),
                isCrysisExpansion ? List.of() : achievementsFactory.createAwards(Constants.ACHIEVEMENTS_SIZE),
                isCrysisExpansion ? List.of() : achievementsFactory.createMilestones(Constants.ACHIEVEMENTS_SIZE),
                gameParameters.getComputers(),
                gameParameters.getExpansions(),
                gameParameters.isDummyHand(),
                dummyHand,
                crysisDeck
        );

    }

}
