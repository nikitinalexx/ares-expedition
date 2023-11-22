package com.terraforming.ares.services.ai.network.activationFunctions;


import com.terraforming.ares.services.ai.network.DataColumn;

public interface Signal {
    float getValue(float x);

    void apply(DataColumn dataColumn, int index);

    void apply(DataColumn dataColumn, int from, int to);

}
