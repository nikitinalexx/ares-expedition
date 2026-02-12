package com.terraforming.ares.services.ai.advanced.features.hand;

public class Stats {
    public float sum;
    public float max;
    public int nonZero;

    void add(float v) {
        sum += v;
        if (v > max) max = v;
        if (v != 0f) nonZero++;
    }
}
