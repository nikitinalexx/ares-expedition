package com.terraforming.ares.model.parameters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeasurableGlobalParameter {
    @Singular
    @Getter
    private List<ParameterGradation> levels;
    private int currentLevel;

    public MeasurableGlobalParameter(MeasurableGlobalParameter copy) {
        this.levels = new ArrayList<>(copy.levels);
        this.currentLevel = copy.currentLevel;
    }

    @JsonIgnore
    public int getCurrentValue() {
        return levels.get(currentLevel).getValue();
    }

    @JsonIgnore
    public int valueLeft() {
        return levels.size() - currentLevel - 1;
    }

    @JsonIgnore
    public ParameterColor getCurrentColor() {
        return levels.get(currentLevel).getColor();
    }

    @JsonIgnore
    public boolean isMax() {
        return currentLevel == levels.size() - 1;
    }

    @JsonIgnore
    public boolean isMin() {
        return currentLevel == 0;
    }

    @JsonIgnore
    public int getMin() {
        return levels.get(0).getValue();
    }

    public void increase() {
        if (!isMax()) {
            currentLevel++;
        }
    }

    public void reduce() {
        if (!isMin()) {
            currentLevel--;
        }
    }
}
