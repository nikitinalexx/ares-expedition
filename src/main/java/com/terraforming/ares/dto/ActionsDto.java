package com.terraforming.ares.dto;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 30.05.2022
 */
@Value
@Builder
public class ActionsDto {
    @Singular
    Map<String, String> playersToNextActions;
}
