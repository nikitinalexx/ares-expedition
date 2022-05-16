package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.parameters.Ocean;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Service
@RequiredArgsConstructor
public class TerraformingService {
    private final CardService cardService;

    public boolean canRevealOcean(MarsGame game) {
        return !game.getPlanetAtTheStartOfThePhase().allOceansRevealed();
    }

    public boolean canIncreaseTemperature(MarsGame game) {
        return !game.getPlanetAtTheStartOfThePhase().isTemperatureMax();
    }

    public boolean canIncreaseOxygen(MarsGame game) {
        return !game.getPlanetAtTheStartOfThePhase().isOxygenMax();
    }

    public void revealOcean(MarsGame game, Player player) {
        if (!canRevealOcean(game)) {
            return;
        }

        Ocean ocean = game.getPlanet().revealOcean();

        player.setMc(player.getMc() + ocean.getMc());
        player.setPlants(player.getPlants() + ocean.getPlants());

        if (ocean.getCards() != 0) {
            for (Integer card : game.dealCards(ocean.getCards())) {
                player.getHand().addCard(card);
            }
        }

        player.setTerraformingRating(player.getTerraformingRating() + 1);

        player.getPlayed().getCards().stream().map(cardService::getCard).forEach(
                projectCard -> projectCard.onOceanFlippedEffect(player)
        );
    }

    public void raiseOxygen(MarsGame game, Player player) {
        if (!canIncreaseOxygen(game)) {
            return;
        }

        game.getPlanet().increaseOxygen();

        player.setTerraformingRating(player.getTerraformingRating() + 1);

        player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .forEach(project -> project.onOxygenChangedEffect(player));
    }

    public void increaseTemperature(MarsGame game, Player player) {
        if (!canIncreaseTemperature(game)) {
            return;
        }

        game.getPlanet().increaseTemperature();

        player.setTerraformingRating(player.getTerraformingRating() + 1);

        player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .forEach(project -> project.onTemperatureChangedEffect(player));
    }

    public void buildForest(MarsGame game, Player player) {
        if (canIncreaseOxygen(game)) {
            game.getPlanet().increaseOxygen();

            player.setTerraformingRating(player.getTerraformingRating() + 1);

            player.getPlayed()
                    .getCards()
                    .stream()
                    .map(cardService::getCard)
                    .forEach(project -> project.onOxygenChangedEffect(player));
        }

        player.setForests(player.getForests() + 1);

        player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .forEach(project -> project.onForestBuiltEffect(player));
    }

}
