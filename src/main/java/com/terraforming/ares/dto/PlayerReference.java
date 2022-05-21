package com.terraforming.ares.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 21.05.2022
 */
@Builder
@Getter
public class PlayerReference {
    private final String uuid;
    private final String name;
}
