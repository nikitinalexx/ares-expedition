package com.terraforming.ares.services.ai.network.activationFunctions;


import com.terraforming.ares.services.ai.network.DataColumn;

import java.io.Serializable;

public class SigSignal implements Signal, Serializable {

    public float getValue(float x) {
        return 1.0F / (1.0F + (float) Math.exp(-x));
    }

    public void apply(DataColumn dataColumn, int index) {
        float[] values = dataColumn.getValues();
        int rowsCols = dataColumn.getRows() * dataColumn.getCols();
        int start = rowsCols * index;
        int end = start + rowsCols;

        for (int i = start; i < end; ++i) {
            values[i] = 1.0F / (1.0F + (float) Math.exp(-values[i]));
        }

    }

    public void apply(DataColumn dataColumn, int from, int to) {
        float[] values = dataColumn.getValues();

        for (int i = from; i < to; ++i) {
            values[i] = 1.0F / (1.0F + (float) Math.exp(-values[i]));
        }
    }

}
