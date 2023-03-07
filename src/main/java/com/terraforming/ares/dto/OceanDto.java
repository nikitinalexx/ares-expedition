package com.terraforming.ares.dto;

import com.terraforming.ares.model.parameters.Ocean;
import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 12.05.2022
 */
@Value
public class OceanDto {
    int cards;
    int mc;
    int plants;
    boolean revealed;

    public static OceanDto of(Ocean ocean) {
        return new OceanDto(
                ocean.getCards(),
                ocean.getMc(),
                ocean.getPlants(),
                ocean.isRevealed()
        );
    }
}
