package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;

public interface PolicyFeatureBlock {

    void encode(TableContext ctx, PolicyRecord dto);

}

