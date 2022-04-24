package com.alex.nikitin.terraformingm.model.parameters;

import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Value
public class ParameterGradation {
    int value;
    ParameterColor color;

    public static ParameterGradation of(int value, ParameterColor color) {
        return new ParameterGradation(value, color);
    }
}
