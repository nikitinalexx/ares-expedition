package com.terraforming.ares.model.parameters;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterGradation {
    private int value;
    private ParameterColor color;

    public static ParameterGradation of(int value, ParameterColor color) {
        return new ParameterGradation(value, color);
    }
}
