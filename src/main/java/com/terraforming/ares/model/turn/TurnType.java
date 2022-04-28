package com.terraforming.ares.model.turn;

import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Getter
public enum TurnType {
    PICK_CORPORATION(true),
    PICK_STAGE(true),
    BUILD_GREEN_PROJECT(true),
    SELL_CARDS(false),
    SKIP_TURN(true);

    boolean isTerminal;

    TurnType(boolean isTerminal) {
        this.isTerminal = isTerminal;
    }
}
