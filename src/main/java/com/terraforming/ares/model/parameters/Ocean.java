package com.terraforming.ares.model.parameters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Ocean {
    //7-9 white, 4-6 yellow, 2-3 red, 0-1 purple
    private boolean revealed;
    private int cards;
    private int mc;
    private int plants;

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

    public void hide() {
        revealed = false;
    }

    @JsonIgnore
    public int key() {
        return (getCards() << 16) | (getMc() << 8) | getPlants();
    }

}
