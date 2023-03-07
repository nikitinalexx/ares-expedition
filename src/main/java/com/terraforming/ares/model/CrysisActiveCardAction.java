package com.terraforming.ares.model;

import com.terraforming.ares.model.turn.TurnType;

/**
 * Created by oleksii.nikitin
 * Creation date 04.03.2023
 */
public enum CrysisActiveCardAction {
    PLANTS_INTO_TOKENS(TurnType.PLANTS_TO_CRISIS_TOKEN),
    HEAT_INTO_TOKENS(TurnType.HEAT_TO_CRISIS_TOKEN),
    CARDS_INTO_TOKENS(TurnType.CARDS_TO_CRISIS_TOKEN);

    private final TurnType turnType;

    CrysisActiveCardAction(TurnType turnType) {
        this.turnType = turnType;
    }

    public TurnType getTurnType() {
        return turnType;
    }
}
