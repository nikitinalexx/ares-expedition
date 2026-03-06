package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

public interface EffectDecision {
    HeadAction getChosenAction();
    void applyRollout(PolicyRecord record);
}
