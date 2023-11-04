package com.terraforming.ares.dto;

import com.terraforming.ares.model.DetrimentToken;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

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
    private ParameterColor phaseTemperatureColor;
    private ParameterColor phaseOxygenColor;
    private List<OceanDto> oceans;
    private int turns;
    private boolean dummyHandMode;
    private List<Integer> usedDummyHand;
    private List<AwardDto> awards;
    private List<MilestoneDto> milestones;
    private CrysisDto crysisDto;
    private String stateReason;
}
