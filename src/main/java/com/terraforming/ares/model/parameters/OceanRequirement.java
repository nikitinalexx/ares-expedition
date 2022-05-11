package com.terraforming.ares.model.parameters;

import lombok.Builder;
import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 11.05.2022
 */
@Builder
@Value
public class OceanRequirement {
    int minValue;
    int maxValue;
}
