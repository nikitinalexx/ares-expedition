package com.terraforming.ares.services.ai.dto;

import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import lombok.Builder;
import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 28.03.2023
 */
@Builder
@Getter
public class ProjectionWithGame {
    public static final ProjectionWithGame SKIP_PHASE = ProjectionWithGame.builder()
            .projection(PhaseChoiceProjection.SKIP_PHASE)
            .build();
    PhaseChoiceProjection projection;
    MarsGame game;
    MarsGameRowDifference difference;

    public boolean isPickPhase() {
        return projection.isPickPhase();
    }
}
