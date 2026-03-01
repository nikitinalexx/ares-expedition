package com.terraforming.ares.services.ai.advanced;

import lombok.Getter;

public class FeatureWriter {
    @Getter
    private final float[] data;
    @Getter
    private int index;

    public FeatureWriter(int size) {
        data = new float[size];
    }

    public void write(float value) {
        data[index++] = value;
    }

    public void skip(int count) {
        index += count;
    }

}
