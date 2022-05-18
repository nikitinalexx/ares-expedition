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
public class GameDto {
    private PlayerDto player;
    private List<AnotherPlayerDto> otherPlayers;
    private Integer phase;
    private int temperature;
    private int oxygen;
    private Integer phaseTemperature;
    private Integer phaseOxygen;
    private Integer phaseOceans;
    private List<OceanDto> oceans;
}
