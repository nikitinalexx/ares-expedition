package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

public interface PlayerPolicyFeatureBlock {

    void encode(TableContext ctx, PolicyRecord dto, int playerIndex);

}

