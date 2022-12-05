package com.terraforming.ares.services.ai.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 05.12.2022
 */
@Builder
@Getter
public class ReducedCardValue {
    private static final ReducedCardValue nonReducedValue = ReducedCardValue.builder().isValueReduced(false).build();
    boolean isValueReduced;
    float value;

    public static ReducedCardValue noReduce() {
        return nonReducedValue;
    }

    public static ReducedCardValue useReducedValue(float value) {
        return ReducedCardValue.builder().isValueReduced(true).value(value).build();
    }
}
