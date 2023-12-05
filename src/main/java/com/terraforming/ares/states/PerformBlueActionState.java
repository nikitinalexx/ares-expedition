package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.StateTransitionService;
import com.terraforming.ares.services.TerraformingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class PerformBlueActionState extends AbstractState {

    public PerformBlueActionState(MarsContext context, StateTransitionService stateTransitionService) {
        super(context, stateTransitionService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        final MarsGame marsGame = context.getGame();
        final TerraformingService terraformingService = context.getTerraformingService();
        Player player = marsGame.getPlayerByUuid(stateContext.getPlayerUuid());

        if (player.getNextTurn() != null && stateContext.getTurnTypeService().isIntermediate(player.getNextTurn().getType())) {
            return List.of(player.getNextTurn().getType());
        } else if (player.getNextTurn() != null) {
            return List.of();
        } else {
            List<TurnType> turns = new ArrayList<>();

            if (player.canBuildBlueRed()) {
                turns.add(TurnType.BUILD_BLUE_RED_PROJECT);
            }

            if (player.canBuildGreen()) {
                turns.add(TurnType.BUILD_GREEN_PROJECT);
            }

            turns.addAll(List.of(
                    TurnType.PERFORM_BLUE_ACTION,
                    TurnType.SELL_CARDS,
                    TurnType.STANDARD_PROJECT,
                    TurnType.EXCHANGE_HEAT
            ));

            if (isSellVpTurnAvailable()) {
                turns.add(TurnType.SELL_VP);
            }

            int forestPlantCost = stateContext.getPaymentValidationService().forestPriceInPlants(player);

            if (player.getPlants() >= forestPlantCost) {
                turns.add(TurnType.PLANT_FOREST);
            }
            if (player.getHeat() >= Constants.TEMPERATURE_HEAT_COST && terraformingService.canIncreaseTemperature(marsGame)) {
                turns.add(TurnType.INCREASE_TEMPERATURE);
            }
            if (marsGame.getExpansions().contains(Expansion.INFRASTRUCTURE) && terraformingService.canIncreaseInfrastructure(marsGame)) {
                if (player.getHeat() >= Constants.INFRASTRUCTURE_HEAT_COST && player.getPlants() >= Constants.INFRASTRUCTURE_PLANT_COST) {
                    turns.add(TurnType.INCREASE_INFRASTRUCTURE);
                }
            }

            if (marsGame.isCrysis() || player.getPlants() < forestPlantCost && (player.getHeat() < Constants.TEMPERATURE_HEAT_COST || marsGame.getPlanetAtTheStartOfThePhase().isTemperatureMax())) {
                turns.add(TurnType.SKIP_TURN);
            }

            if (marsGame.isCrysis()) {
                final CardService cardService = context.getCardService();
                marsGame.getCrysisData()
                        .getOpenedCards()
                        .stream()
                        .map(cardService::getCrysisCard)
                        .map(CrysisCard::getActiveCardAction)
                        .filter(Objects::nonNull)
                        .map(CrysisActiveCardAction::getTurnType)
                        .forEach(turns::add);
            }

            if (marsGame.gameEndCondition()) {
                turns.remove(TurnType.SKIP_TURN);
                turns.add(TurnType.GAME_END_CONFIRM);
            }

            if (player.isUnmiCorporation() && player.isHasUnmiAction()) {
                turns.add(TurnType.UNMI_RT);
            }

            return turns;
        }
    }

    @Override
    public void updateState() {
        final MarsGame marsGame = context.getGame();
        boolean gameNotEndedOrPlayersConfirmedEnd = !marsGame.gameEndCondition() || marsGame.getPlayerUuidToPlayer().values().stream().allMatch(Player::isConfirmedGameEndThirdPhase);
        boolean noPendingTurns = marsGame.getPlayerUuidToPlayer()
                .values().stream().allMatch(
                        player -> player.getNextTurn() == null
                );

        if (gameNotEndedOrPlayersConfirmedEnd && noPendingTurns) {
            stateTransitionService.performStateTransferFromPhase(marsGame, 4);
        }
    }
}
