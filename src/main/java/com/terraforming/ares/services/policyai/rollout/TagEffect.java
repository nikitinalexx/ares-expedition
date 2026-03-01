package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;

public interface TagEffect {
    boolean isPresent();
    void prepareContext(PolicyRecord record);      // при смене эффекта
    List<EffectDecision> resolveDecisions(EffectStateReader state);
    void clearContext(PolicyRecord record);

    // Теперь принимает record, не player
    default void applySideEffect(PolicyRecord record) {}
}
