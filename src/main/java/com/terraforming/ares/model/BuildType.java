package com.terraforming.ares.model;

/**
 * Created by oleksii.nikitin
 * Creation date 27.02.2023
 */
public enum BuildType {
    GREEN(true, false),
    BLUE_RED(false, true),
    BLUE_RED_OR_CARD(false, true),
    BLUE_RED_OR_MC(false, true),
    GREEN_OR_BLUE(true, true);

    private final boolean isGreen;
    private final boolean isBlueRed;

    BuildType(boolean isGreen, boolean isBlueRed) {
        this.isGreen = isGreen;
        this.isBlueRed = isBlueRed;
    }

    public boolean isGreen() {
        return isGreen;
    }

    public boolean isBlueRed() {
        return isBlueRed;
    }
}
