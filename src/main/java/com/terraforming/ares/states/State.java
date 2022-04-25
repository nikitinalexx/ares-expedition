package com.terraforming.ares.states;

import com.terraforming.ares.model.turn.TurnType;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
public interface State {

    Action nextAction(String playerUuid);

    List<TurnType> getPossibleTurns(String playerUuid);

    void updateState();
}
