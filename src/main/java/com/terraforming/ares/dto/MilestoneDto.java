package com.terraforming.ares.dto;

import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.model.milestones.MilestoneType;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
@Builder
@Getter
public class MilestoneDto {
    private final MilestoneType type;
    private final Set<String> players;

    public static MilestoneDto from(Milestone milestone) {
        return MilestoneDto.builder()
                .type(milestone.getType())
                .players(milestone.getAchievedByPlayers())
                .build();
    }
}
