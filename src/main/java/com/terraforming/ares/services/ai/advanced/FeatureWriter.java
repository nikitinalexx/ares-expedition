package com.terraforming.ares.services.ai.advanced;

public class FeatureWriter {
    private final float[] data;
    private int index;

    public FeatureWriter(int size) {
        data = new float[size];
    }

    /**
     * Wraps an EXISTING buffer and rewinds it, so a per-thread buffer can be reused across observations. The observation is
     * built on every candidate of every decision (hundreds of times per game, thousands in a rollout); allocating a fresh
     * float[] each time is pure GC pressure (owner's hint, 19/11: the vector should be fast to fill).
     */
    public FeatureWriter(float[] buffer) {
        this.data = buffer;
        this.index = 0;
    }

    /** Rewinds to the start, keeping the buffer (used with {@link #FeatureWriter(float[])}). */
    public FeatureWriter reset() {
        this.index = 0;
        return this;
    }

    public int position() {
        return index;
    }

    public void write(float value) {
        data[index++] = value;
    }

    public float[] getData() {
        return data;
    }

}
