package com.terraforming.ares.model.turn;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
public interface Turn {

    String getPlayerUuid();

    TurnType getType();

}
