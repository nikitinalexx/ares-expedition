package com.terraforming.ares.services.ai.advanced;

public class FeatureWriter {
    private final float[] data;
    private int index;

    public FeatureWriter(int size) {
        data = new float[size];
    }

    public void write(float value) {
        data[index++] = value;
    }

    public float[] getData() {
        return data;
    }

}
