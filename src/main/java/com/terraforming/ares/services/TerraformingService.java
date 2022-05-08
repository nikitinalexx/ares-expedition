package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
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

    //TODO implement, true even if maxed in this phase, but not in the next
    public boolean canRevealOcean() {
        return true;
    }

    public boolean canRaiseTemperature() {
        return true;
    }

    public boolean canRaiseOxygen() {
        return true;
    }

    public void revealOcean(MarsGame game, Player player) {
        if (!canRevealOcean()) {
            return;
        }

        Ocean ocean = game.getPlanet().revealOcean().orElseThrow(() -> new IllegalStateException("Trying to flip an ocean when no oceans left"));

        player.setMc(player.getMc() + ocean.getMc());
        player.setPlants(player.getPlants() + ocean.getPlants());

        if (ocean.getCards() != 0) {
            Deck deck = game.getProjectsDeck().dealCards(ocean.getCards());

            for (Integer card : deck.getCards()) {
                player.getHand().addCard(card);
            }
        }

        player.setTerraformingRating(player.getTerraformingRating() + 1);

        player.getPlayed().getCards().stream().map(cardService::getProjectCard).forEach(
                projectCard -> projectCard.onOceanFlippedEffect(player)
        );
    }

    public void raiseOxygen(MarsGame game, Player player) {
        if (!canRaiseOxygen()) {
            return;
        }

        game.getPlanet().increaseOxygen();

        player.setTerraformingRating(player.getTerraformingRating() + 1);

        player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getProjectCard)
                .forEach(project -> project.onOxygenChangedEffect(player));
    }

    public void raiseTemperature(MarsGame game, Player player) {
        if (!canRaiseTemperature()) {
            return;
        }

        game.getPlanet().increaseTemperature();

        player.setTerraformingRating(player.getTerraformingRating() + 1);

        player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getProjectCard)
                .forEach(project -> project.onTemperatureChangedEffect(player));
    }

    public void buildForest(MarsGame game, Player player) {
        if (canRaiseOxygen()) {
            game.getPlanet().increaseOxygen();
            player.setTerraformingRating(player.getTerraformingRating() + 1);

            player.getPlayed()
                    .getCards()
                    .stream()
                    .map(cardService::getProjectCard)
                    .forEach(project -> project.onOxygenChangedEffect(player));
        }

        player.setForests(player.getForests() + 1);

        player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getProjectCard)
                .forEach(project -> project.onForestBuiltEffect(player));
    }

}
