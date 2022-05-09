package com.terraforming.ares.model.parameters;

import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Getter
public class Ocean {
    private boolean revealed;
    private final int cards;
    private final int mc;
    private final int plants;

    public Ocean(Ocean copy) {
        this.revealed = copy.revealed;
        this.cards = copy.cards;
        this.mc = copy.mc;
        this.plants = copy.plants;
    }

    public Ocean(int cards, int mc, int plants) {
        this.cards = cards;
        this.mc = mc;
        this.plants = plants;
    }

    public void reveal() {
        revealed = true;
    }
}
