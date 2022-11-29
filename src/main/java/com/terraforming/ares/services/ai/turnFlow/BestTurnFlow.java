package com.terraforming.ares.services.ai.turnFlow;

import com.terraforming.ares.model.Card;
import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 29.11.2022
 */
@Getter
public class BestTurnFlow {
    private float bestChance;
    private BestTurnType bestTurnType;
    private Card card;

    public BestTurnFlow(float initialChance) {
        this.bestChance = initialChance;
        this.bestTurnType = BestTurnType.SKIP;
    }

    public void addScenarioToFlow(float chance, BestTurnType type) {
        if (chance > bestChance) {
            bestChance = chance;
            bestTurnType = type;
        }
    }

    public void addScenarioToFlow(float chance, BestTurnType type, Card card) {
        if (chance > bestChance) {
            bestChance = chance;
            bestTurnType = type;
            this.card = card;
        }
    }

}
