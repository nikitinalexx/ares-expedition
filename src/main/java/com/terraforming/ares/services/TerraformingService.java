package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.parameters.Ocean;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Service
@RequiredArgsConstructor
public class TerraformingService {
    private final CardService cardService;

    public boolean canRevealOcean(MarsGame game) {
        return !game.getPlanetAtTheStartOfThePhase().allOceansRevealed() || game.isCrysis();
    }

    public boolean canIncreaseTemperature(MarsGame game) {
        return !game.getPlanetAtTheStartOfThePhase().isTemperatureMax() || game.isCrysis();
    }

    public boolean canIncreaseOxygen(MarsGame game) {
        return !game.getPlanetAtTheStartOfThePhase().isOxygenMax() || game.isCrysis();
    }

    public boolean canReduceOxygen(MarsGame game) {
        return !game.getPlanet().isOxygenMin();
    }

    public boolean canReduceTemperature(MarsGame game) {
        return !game.getPlanet().isTemperatureMin();
    }

    public void revealOcean(MarsContext context) {
        final MarsGame game = context.getGame();
        final Player player = context.getPlayer();
        if (!canRevealOcean(game)) {
            return;
        }

        Ocean ocean = game.getPlanet().revealOcean();

        player.setMc(player.getMc() + ocean.getMc());
        player.setPlants(player.getPlants() + ocean.getPlants());

        if (ocean.getCards() != 0) {
            for (Integer card : cardService.dealCards(game, ocean.getCards())) {
                player.getHand().addCard(card);
            }
        }

        player.setTerraformingRating(player.getTerraformingRating() + 1);

        player.getPlayed().getCards().stream().map(cardService::getCard).forEach(
                projectCard -> projectCard.onOceanFlippedEffect(context)
        );

        if (game.isCrysis()) {
            final List<Integer> openedCards = new ArrayList<>(game.getCrysisData().getOpenedCards());
            openedCards
                    .stream()
                    .map(cardService::getCrysisCard)
                    .forEach(card -> card.onOceanFlippedEffect(context));
        }
    }

    public void hideOcean(MarsGame game, int index) {
        if (!game.getPlanet().canHideOcean(index)) {
            return;
        }

        game.getPlanet().hideOcean(index);
    }

    public void raiseOxygen(MarsContext context) {
        final MarsGame game = context.getGame();
        final Player player = context.getPlayer();
        if (!canIncreaseOxygen(game)) {
            return;
        }

        game.getPlanet().increaseOxygen();

        player.setTerraformingRating(player.getTerraformingRating() + 1);

        onOxygenChangedEffect(context);
    }

    public void reduceOxygen(MarsContext context) {
        final MarsGame game = context.getGame();
        if (!canReduceOxygen(game)) {
            game.setStateType(StateType.GAME_END, context, "No more Oxygen left on Mars");
            return;
        }

        game.getPlanet().reduceOxygen();
    }

    public void increaseTemperature(MarsContext context) {
        final MarsGame game = context.getGame();
        final Player player = context.getPlayer();
        if (!canIncreaseTemperature(game)) {
            return;
        }

        game.getPlanet().increaseTemperature();

        player.setTerraformingRating(player.getTerraformingRating() + 1);

        player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .forEach(project -> project.onTemperatureChangedEffect(context));

        if (game.isCrysis()) {
            final List<Integer> openedCards = new ArrayList<>(game.getCrysisData().getOpenedCards());
            openedCards
                    .stream()
                    .map(cardService::getCrysisCard)
                    .forEach(card -> card.onTemperatureChangedEffect(context));
        }
    }

    public void reduceTemperature(MarsContext context) {
        final MarsGame game = context.getGame();
        if (!canReduceTemperature(game)) {
            game.setStateType(StateType.GAME_END, context, "Mars became too cold");
            return;
        }

        game.getPlanet().reduceTemperature();
    }

    public void buildForest(MarsContext context) {
        final MarsGame game = context.getGame();
        final Player player = context.getPlayer();

        if (canIncreaseOxygen(game)) {
            game.getPlanet().increaseOxygen();

            player.setTerraformingRating(player.getTerraformingRating() + 1);

            onOxygenChangedEffect(context);
        }

        player.setForests(player.getForests() + 1);

        player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .forEach(project -> project.onForestBuiltEffect(context));
    }

    private void onOxygenChangedEffect(MarsContext context) {
        final MarsGame game = context.getGame();
        final Player player = context.getPlayer();
        player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .forEach(project -> project.onOxygenChangedEffect(context));

        if (game.isCrysis()) {
            final List<Integer> openedCards = new ArrayList<>(game.getCrysisData().getOpenedCards());
            openedCards
                    .stream()
                    .map(cardService::getCrysisCard)
                    .forEach(card -> card.onOxygenChangedEffect(context));
        }
    }

}
