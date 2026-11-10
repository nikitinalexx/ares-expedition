package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.GlobalParameter;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.parameters.Ocean;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.policyai.GameArenaContext;
import com.terraforming.ares.services.policyai.dto.GameRecordArena;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
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

    public boolean canIncreaseInfrastructure(MarsGame game) {
        return !game.getPlanetAtTheStartOfThePhase().isInfrastructureMax();
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

        onParameterRaised(player, null);

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

        onParameterRaised(player, GlobalParameter.OXYGEN);

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

        onParameterRaised(player, GlobalParameter.TEMPERATURE);


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

    public void increaseInfrastructure(MarsContext context) {
        final MarsGame game = context.getGame();
        final Player player = context.getPlayer();
        if (!canIncreaseInfrastructure(game)) {
            return;
        }

        game.getPlanet().increaseInfrastructure();

        player.setTerraformingRating(player.getTerraformingRating() + 1);

        player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .forEach(project -> project.onInfrastructureChangedEffect(context));

        player.getHand().addCard(cardService.dealCards(game, 1).getFirst());
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

            onParameterRaised(player, GlobalParameter.OXYGEN);

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

    private void onParameterRaised(Player player, GlobalParameter globalParameter) {
        if (!GameArenaContext.isArenaAvailable()) {
            return;
        }

        if (!AiConstants.POLICY_AVAILABLE) {
            return;
        }

        GameRecordArena arena = GameArenaContext.current();

        for (int i = arena.size() - 1; i >= 0; i--) {
            PolicyRecord record = arena.records()[i];

            if (player.isFirstBot() && record.isFirstPlayer || !player.isFirstBot() && !record.isFirstPlayer) {
                continue;
            }

            if (record.finalizedDelta) {
                return;
            }

            if (record.marsReference == null) {
                continue;
            }


            switch (globalParameter) {
                case OXYGEN -> record.opponentDidOxygen++;
                case TEMPERATURE -> record.opponentDidTemperature++;
                case null -> {
                    record.opponentOpenedOceans++;
                }
                case INFRASTRUCTURE -> {
                }
            }
        }
    }

}
