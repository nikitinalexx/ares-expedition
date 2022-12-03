package com.terraforming.ares.services.ai.dto;

import com.terraforming.ares.model.Card;
import lombok.Builder;
import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 29.11.2022
 */
@Value
@Builder
public class BuildProjectPrediction {
    boolean canBuild;
    Card card;
    float expectedValue;
}
