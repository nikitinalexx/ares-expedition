package com.terraforming.ares.model;

import com.terraforming.ares.model.parameters.MeasurableGlobalParameter;
import com.terraforming.ares.model.parameters.Ocean;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.AllArgsConstructor;
import lombok.Builder;

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
public class Planet {
    private final Map<GlobalParameter, MeasurableGlobalParameter> measurableGlobalParameters;
    private final List<Ocean> oceans;

    public Planet(Planet copy) {
        this.measurableGlobalParameters =
                copy.measurableGlobalParameters.entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey, entry -> new MeasurableGlobalParameter(entry.getValue()))
                );

        this.oceans = copy.oceans.stream().map(Ocean::new).collect(Collectors.toList());
    }

    public boolean allOceansRevealed() {
        return oceans.stream().allMatch(Ocean::isRevealed);
    }

    public boolean isTemperatureMax() {
        return measurableGlobalParameters.get(GlobalParameter.TEMPERATURE).isMax();
    }

    public boolean isOxygenMax() {
        return measurableGlobalParameters.get(GlobalParameter.OXYGEN).isMax();
    }

    public boolean isValidTemperatute(List<ParameterColor> validParameters) {
        return isValidParameter(validParameters, GlobalParameter.TEMPERATURE);
    }

    public boolean isValidOxygen(List<ParameterColor> validParameters) {
        return isValidParameter(validParameters, GlobalParameter.OXYGEN);
    }

    public boolean isValidNumberOfOceans(OceanRequirement oceanRequirement) {
        if (oceanRequirement == null) {
            return true;
        }
        int oceansOpened = (int) oceans.stream().filter(Ocean::isRevealed).count();

        return oceansOpened >= oceanRequirement.getMinValue() && oceansOpened <= oceanRequirement.getMaxValue();
    }

    public Ocean revealOcean() {
        Optional<Ocean> result = oceans.stream().filter(ocean -> !ocean.isRevealed()).findAny();
        result.ifPresent(Ocean::reveal);
        return result.orElse(oceans.get(oceans.size() - 1));
    }

    public void increaseTemperature() {
        measurableGlobalParameters.get(GlobalParameter.TEMPERATURE).increase();
    }

    public void increaseOxygen() {
        measurableGlobalParameters.get(GlobalParameter.OXYGEN).increase();
    }

    private boolean isValidParameter(List<ParameterColor> validColors, GlobalParameter globalParameter) {
        if (validColors.isEmpty()) {
            return true;
        }

        return validColors.contains(measurableGlobalParameters.get(globalParameter).getCurrentColor());
    }

}
