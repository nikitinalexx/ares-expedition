package com.terraforming.ares.services.ai.network.activationFunctions;


import com.terraforming.ares.services.ai.network.DataColumn;

import java.io.Serializable;

public class SimpleSignal implements Signal, Serializable {

    public float getValue(float x) {
        return x;
    }

    public void apply(DataColumn dataColumn, int channel) {
    }

    public void apply(DataColumn dataColumn, int from, int to) {
    }
}
