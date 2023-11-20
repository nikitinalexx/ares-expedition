package com.terraforming.ares.model.income;

import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 09.05.2022
 */
@Value
public class Gain {
    public static final Gain NONE = Gain.of(null, 0);
    GainType type;
    int value;

    public static Gain of(GainType type, int value) {
        return new Gain(type, value);
    }

}
