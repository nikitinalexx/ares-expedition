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
    private List<PlayerDto> otherPlayers;
    private Integer phase;
    private int temperature;
    private int oxygen;
    private List<OceanDto> oceans;
}
