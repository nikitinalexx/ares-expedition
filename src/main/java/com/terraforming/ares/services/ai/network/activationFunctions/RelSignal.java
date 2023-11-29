package com.terraforming.ares.services.ai.network.activationFunctions;


import com.terraforming.ares.services.ai.network.DataColumn;

import java.io.Serializable;

public class RelSignal implements Signal, Serializable {

    public float getValue(float x) {
        return Math.max(0.0F, x);
    }

    public void apply(DataColumn dataColumn, int index) {
        float[] values = dataColumn.getValues();
        int rowsCols = dataColumn.getRows() * dataColumn.getCols();
        int start = rowsCols * index;
        int end = start + rowsCols;

        for (int i = start; i < end; ++i) {
            values[i] = Math.max(0.0F, values[i]);
        }

    }

    public void apply(DataColumn dataColumn, int from, int to) {
        float[] values = dataColumn.getValues();

        for (int i = from; i < to; ++i) {
            values[i] = Math.max(0.0F, values[i]);
        }
    }

}
