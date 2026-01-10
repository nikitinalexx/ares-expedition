package com.terraforming.ares.services.ai.dl4j;

public class Prediction {
    public final double baseProb;

    public Prediction(double baseProb) {
        this.baseProb = baseProb;
    }

    @Override
    public String toString() {
        return String.format("[ORACLE] Base: %.2f%%", baseProb * 100);
    }
}
