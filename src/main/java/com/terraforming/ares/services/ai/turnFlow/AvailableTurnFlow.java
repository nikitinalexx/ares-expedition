package com.terraforming.ares.services.ai.turnFlow;

import com.terraforming.ares.model.Card;
import lombok.Getter;

@Getter
public class AvailableTurnFlow {
    private BestTurnType bestTurnType;
    private Card card;

    public AvailableTurnFlow() {
        this.bestTurnType = BestTurnType.SKIP;
    }

    public void addScenarioToFlow(BestTurnType type) {
        bestTurnType = type;
    }

    public void addScenarioToFlow(BestTurnType type, Card card) {
        bestTurnType = type;
        this.card = card;
    }

    public boolean nonSkipTurnAvailable() {
        return bestTurnType != BestTurnType.SKIP;
    }

}
