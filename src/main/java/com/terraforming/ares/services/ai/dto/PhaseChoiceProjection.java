package com.terraforming.ares.services.ai.dto;

import lombok.*;

@Builder
@Getter
@Setter
public class PhaseChoiceProjection {
    public static final PhaseChoiceProjection SKIP_PHASE = PhaseChoiceProjection.builder().pickPhase(false).build();

    private boolean pickPhase;
    private int phase;
    private float chance;

    public PhaseChoiceProjection applyIfBetter(PhaseChoiceProjection projection) {
        if (!projection.isPickPhase()) {
            return this;
        }
        if (!pickPhase || projection.chance > chance) {
            return projection;
        }
        return this;
    }

    public PhaseChoiceProjection bestOfTheWorst(PhaseChoiceProjection projection) {
        if (pickPhase || projection.pickPhase) {
            throw new IllegalStateException("Can't choose worst of good phase projections");
        }
        if (projection.chance > this.chance) {
            return projection;
        }
        return this;
    }
}
