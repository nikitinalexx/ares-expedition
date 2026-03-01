package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.services.policyai.action.ActionRegistry;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

// Читает из record (когда предыдущие эффекты уже сделали rollout)
public class RecordStateReader implements EffectStateReader {
    private final PolicyRecord record;

    public RecordStateReader(PolicyRecord record) {
        this.record = record;
    }

    @Override
    public int getMicrobeCount(Class<?> cardClass) {
        return record.microbeResources[0][ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(cardClass)];
    }

}
