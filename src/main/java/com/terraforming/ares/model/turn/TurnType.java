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
    SKIP_TURN(true),
    BUILD_BLUE_RED_PROJECT(true),
    PERFORM_BLUE_ACTION(false),
    COLLECT_INCOME(true),
    DRAFT_CARDS(true)
    ;

    boolean isTerminal;

    TurnType(boolean isTerminal) {
        this.isTerminal = isTerminal;
    }
}
