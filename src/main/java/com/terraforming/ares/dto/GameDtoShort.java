package com.terraforming.ares.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
@Data
@Builder
public class GameDtoShort {
    private int temperature;
    private int oxygen;
    private Integer infrastructure;
    private Integer phaseTemperature;
    private Integer phaseInfrastructure;
    private Integer phaseOxygen;
    private Integer phaseOceans;
    private List<OceanDto> oceans;
    private List<AnotherPlayerDto> otherPlayers;
}
