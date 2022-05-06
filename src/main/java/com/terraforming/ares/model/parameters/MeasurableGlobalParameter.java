package com.terraforming.ares.model.parameters;

import lombok.Builder;
import lombok.Singular;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
public class MeasurableGlobalParameter {
    @Singular
    private final List<ParameterGradation> levels;
    private int currentLevel;

    public int getCurrentValue() {
        return levels.get(currentLevel).getValue();
    }

    public ParameterColor getCurrentColor() {
        return levels.get(currentLevel).getColor();
    }

    public boolean isMax() {
        return currentLevel == levels.size() - 1;
    }

    public void increase() {
        if (!isMax()) {
            currentLevel++;
        }
    }
}
