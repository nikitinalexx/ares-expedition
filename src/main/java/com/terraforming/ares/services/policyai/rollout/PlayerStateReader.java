package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.model.Player;

// Читает из Player (когда record ещё нет)
public class PlayerStateReader implements EffectStateReader {
    private final Player player;

    public PlayerStateReader(Player player) {
        this.player = player;
    }

    @Override
    public int getMicrobeCount(Class<?> cardClass) {
        return player.getCardResourcesCount().getOrDefault(cardClass, 0);
    }
}


