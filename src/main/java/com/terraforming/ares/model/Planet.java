package com.terraforming.ares.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terraforming.ares.model.parameters.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Planet {
    private Map<GlobalParameter, MeasurableGlobalParameter> measurableGlobalParameters;
    private List<Ocean> oceans;
    private int lastOpenedOceanIndex;

    public Planet(Planet copy) {
        this.measurableGlobalParameters =
                copy.measurableGlobalParameters.entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey, entry -> new MeasurableGlobalParameter(entry.getValue()))
                );

        this.oceans = copy.oceans.stream().map(Ocean::new).collect(Collectors.toList());
        this.lastOpenedOceanIndex = copy.lastOpenedOceanIndex;
    }

    @JsonIgnore
    public int getMinimumTemperature() {
        return measurableGlobalParameters.get(GlobalParameter.TEMPERATURE).getMin();
    }

    public boolean allOceansRevealed() {
        return oceans.stream().allMatch(Ocean::isRevealed);
    }

    public boolean canHideOcean(int index) {
        return index >= 0 && index <= oceans.size() - 1 && oceans.get(index).isRevealed();
    }

    @JsonIgnore
    public int oceansLeft() {
        return (int) oceans.stream().filter(ocean -> !ocean.isRevealed()).count();
    }

    @JsonIgnore
    public int temperatureLeft() {
        return measurableGlobalParameters.get(GlobalParameter.TEMPERATURE).valueLeft();
    }

    @JsonIgnore
    public int oxygenLeft() {
        return measurableGlobalParameters.get(GlobalParameter.OXYGEN).valueLeft();
    }

    @JsonIgnore
    public List<Ocean> getRevealedOceans() {
        return oceans.stream().filter(Ocean::isRevealed).collect(Collectors.toList());
    }

    @JsonIgnore
    public int getTemperatureValue() {
        return measurableGlobalParameters.get(GlobalParameter.TEMPERATURE).getCurrentValue();
    }

    @JsonIgnore
    public int getOxygenValue() {
        return measurableGlobalParameters.get(GlobalParameter.OXYGEN).getCurrentValue();
    }

    @JsonIgnore
    public ParameterColor getTemperatureColor() {
        return measurableGlobalParameters.get(GlobalParameter.TEMPERATURE).getCurrentColor();
    }

    @JsonIgnore
    public ParameterColor getOxygenColor() {
        return measurableGlobalParameters.get(GlobalParameter.OXYGEN).getCurrentColor();
    }

    public List<Ocean> getOceans() {
        return oceans;
    }

    @JsonIgnore
    public boolean isTemperatureMax() {
        return measurableGlobalParameters.get(GlobalParameter.TEMPERATURE).isMax();
    }

    @JsonIgnore
    public boolean isTemperatureMin() {
        return measurableGlobalParameters.get(GlobalParameter.TEMPERATURE).isMin();
    }

    @JsonIgnore
    public boolean isOceansMax() {
        return oceans.stream().allMatch(Ocean::isRevealed);
    }

    @JsonIgnore
    public boolean isOxygenMax() {
        return measurableGlobalParameters.get(GlobalParameter.OXYGEN).isMax();
    }

    @JsonIgnore
    public boolean isOxygenMin() {
        return measurableGlobalParameters.get(GlobalParameter.OXYGEN).isMin();
    }

    @JsonIgnore
    public boolean isValidTemperatute(List<ParameterColor> validParameters) {
        return isValidParameter(validParameters, GlobalParameter.TEMPERATURE);
    }

    @JsonIgnore
    public boolean isValidOcean(ParameterColor parameterColor) {
        final int oceansLeft = oceansLeft();
        final int oceansOpened = oceans.size() - oceansLeft;
        if (parameterColor == ParameterColor.W) {
            return oceansOpened >= 7;
        } else if (parameterColor == ParameterColor.Y) {
            return oceansOpened >= 4 && oceansOpened <= 6;
        } else if (parameterColor == ParameterColor.R) {
            return oceansOpened >= 2 && oceansOpened <= 3;
        } else if (parameterColor == ParameterColor.P) {
            return oceansOpened <= 1;
        }
        throw new IllegalArgumentException("Unreachable ocean check");
    }

    @JsonIgnore
    public boolean isValidOxygen(List<ParameterColor> validParameters) {
        return isValidParameter(validParameters, GlobalParameter.OXYGEN);
    }

    @JsonIgnore
    public boolean isValidNumberOfOceans(OceanRequirement oceanRequirement) {
        if (oceanRequirement == null) {
            return true;
        }
        int oceansOpened = (int) oceans.stream().filter(Ocean::isRevealed).count();

        return oceansOpened >= oceanRequirement.getMinValue() && oceansOpened <= oceanRequirement.getMaxValue();
    }

    public Ocean revealOcean() {
        Optional<Ocean> result = oceans.stream().filter(ocean -> !ocean.isRevealed()).findAny();
        result.ifPresent(ocean -> {
                    ocean.reveal();
                    if (allOceansRevealed()) {
                        lastOpenedOceanIndex = oceans.indexOf(ocean);
                    }
                }
        );
        return result.orElseGet(() -> oceans.get(lastOpenedOceanIndex));
    }

    public boolean hideOcean(int index) {
        if (!canHideOcean(index)) {
            return false;
        }
        oceans.get(index).hide();
        lastOpenedOceanIndex = -1;
        return true;
    }

    public void increaseTemperature() {
        measurableGlobalParameters.get(GlobalParameter.TEMPERATURE).increase();
    }

    public void increaseOxygen() {
        measurableGlobalParameters.get(GlobalParameter.OXYGEN).increase();
    }

    public void reduceOxygen() {
        measurableGlobalParameters.get(GlobalParameter.OXYGEN).reduce();
    }

    public void reduceTemperature() {
        measurableGlobalParameters.get(GlobalParameter.TEMPERATURE).reduce();
    }

    private boolean isValidParameter(List<ParameterColor> validColors, GlobalParameter globalParameter) {
        if (validColors.isEmpty()) {
            return true;
        }

        return validColors.contains(measurableGlobalParameters.get(globalParameter).getCurrentColor());
    }

    @JsonIgnore
    public float getParameterProportionTillMinColor(GlobalParameter globalParameter, ParameterColor color) {
        MeasurableGlobalParameter parameter = measurableGlobalParameters.get(globalParameter);

        int targetLevel = 0;

        List<ParameterGradation> levels = parameter.getLevels();
        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i).getColor() == color) {
                targetLevel = i;
                break;
            }
        }

        int currentValue = parameter.getCurrentValue();

        return (float) currentValue / targetLevel;
    }

}
