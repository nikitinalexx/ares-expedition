package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class PerformBlueActionState extends AbstractState {

    public PerformBlueActionState(MarsGame marsGame) {
        super(marsGame);
    }

    @Override
    public List<TurnType> getPossibleTurns(String playerUuid) {
        Player player = marsGame.getPlayerByUuid(playerUuid);
        if (player.getNextTurn() != null) {
            return Collections.emptyList();
        } else {
            List<TurnType> turns = new ArrayList<>(List.of(
                    TurnType.PERFORM_BLUE_ACTION,
                    TurnType.SELL_CARDS,
                    TurnType.STANDARD_PROJECT,
                    TurnType.EXCHANGE_HEAT
            ));

            if (player.getPlants() < Constants.FOREST_PLANT_COST && (player.getHeat() < Constants.TEMPERATURE_HEAT_COST || marsGame.getPlanetAtTheStartOfThePhase().isTemperatureMax())) {
                turns.add(TurnType.SKIP_TURN);
            } else {
                if (player.getPlants() >= Constants.FOREST_PLANT_COST) {
                    turns.add(TurnType.PLANT_FOREST);
                }
                if (player.getHeat() >= Constants.TEMPERATURE_HEAT_COST && !marsGame.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
                    turns.add(TurnType.INCREASE_TEMPERATURE);
                }
            }
            if (marsGame.gameEndCondition()) {
                turns.remove(TurnType.SKIP_TURN);
                turns.add(TurnType.GAME_END_CONFIRM);
            }

            return turns;
        }
    }

    @Override
    public void updateState() {
        if (!marsGame.gameEndCondition() || marsGame.getPlayerUuidToPlayer().values().stream().allMatch(Player::isConfirmedGameEndThirdPhase)) {
            performStateTransferFromPhase(4);
        }
    }
}
