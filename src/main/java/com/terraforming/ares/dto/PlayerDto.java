package com.terraforming.ares.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
@Builder
@Getter
public class PlayerDto {
    private final String playerUuid;
    private final List<CardDto> corporations;
    private final List<CardDto> hand;
    private final Integer corporationId;
    private final Integer phase;
    private final Integer previousPhase;
}
