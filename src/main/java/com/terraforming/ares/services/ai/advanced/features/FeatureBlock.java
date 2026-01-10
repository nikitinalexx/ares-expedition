package com.terraforming.ares.services.ai.advanced.features;

import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;

import java.util.List;

public interface FeatureBlock {
    int size();
    void encode(TableContext ctx, FeatureWriter out);
    List<String> getFeatureNames();
}

