package com.terraforming.ares.services.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CardProjection {
    public static final CardProjection NOT_USABLE = new CardProjection(false, -1);
    private final boolean usable;
    private final float chance;

    public static CardProjection usable(float chance) {
        return new CardProjection(true, chance);
    }
}
