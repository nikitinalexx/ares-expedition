package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

public class PolicyTableRedCardsFeature implements PolicyFeatureBlock {

    @Override
    public void encode(TableContext ctx, PolicyRecord dto) {
        dto.playedRedCardsMask = ctx.getTableRedCardsMask();
    }

}
