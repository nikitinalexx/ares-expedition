package com.terraforming.ares.services.ai.dto;

import lombok.Getter;

@Getter
public class AvailableTurn {
    private final AvailableTurnType type;
    private Integer card;

    public AvailableTurn(AvailableTurnType type) {
        this.type = type;
    }

    public AvailableTurn(AvailableTurnType type, Integer card) {
        this.type = type;
        this.card = card;
    }

}
