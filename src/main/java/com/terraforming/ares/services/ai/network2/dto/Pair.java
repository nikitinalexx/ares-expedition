package com.terraforming.ares.services.ai.network2.dto;

import com.terraforming.ares.model.Player;

public class Pair {
    private final Player p1;
    private final Player p2;

    public Pair(Player p1, Player p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public static Pair of(Player p1, Player p2) {
        return new Pair(p1, p2);
    }

}
