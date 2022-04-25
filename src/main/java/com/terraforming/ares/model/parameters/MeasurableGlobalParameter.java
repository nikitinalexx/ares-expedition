package com.terraforming.ares.model.parameters;

import lombok.Builder;
import lombok.Singular;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
public class MeasurableGlobalParameter {
    @Singular
    private final List<ParameterGradation> levels;
    private final int currentLevel;
}
