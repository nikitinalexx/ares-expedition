package com.terraforming.ares.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terraforming.ares.model.parameters.MeasurableGlobalParameter;
import com.terraforming.ares.model.parameters.Ocean;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
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

    @JsonIgnore
    public boolean isTemperatureMax() {
        return measurableGlobalParameters.get(GlobalParameter.TEMPERATURE).isMax();
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
    public boolean isValidTemperatute(List<ParameterColor> validParameters) {
        return isValidParameter(validParameters, GlobalParameter.TEMPERATURE);
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
