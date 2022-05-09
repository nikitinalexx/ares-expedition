package com.terraforming.ares.model.parameters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
@AllArgsConstructor
public class MeasurableGlobalParameter {
    @Singular
    private final List<ParameterGradation> levels;
    private int currentLevel;

    public MeasurableGlobalParameter(MeasurableGlobalParameter copy) {
        this.levels = new ArrayList<>(copy.levels);
        this.currentLevel = copy.currentLevel;
    }

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
