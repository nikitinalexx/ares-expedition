package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.services.policyai.dto.PolicyRecord;

public interface EffectDecision {
    int getChosenAction();
    void applyRollout(PolicyRecord record);
}
