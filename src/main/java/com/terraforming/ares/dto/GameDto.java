package com.terraforming.ares.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
@Data
@Builder
public class GameDto {
    private PlayerContextDto player;
    private List<PlayerContextDto> otherPlayers;
}
