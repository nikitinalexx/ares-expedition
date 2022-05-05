package com.terraforming.ares.model;

import com.terraforming.ares.model.parameters.MeasurableGlobalParameter;
import com.terraforming.ares.model.parameters.Ocean;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongPredicate;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
public class Planet {
    private final Map<GlobalParameter, MeasurableGlobalParameter> measurableGlobalParameters;
    private final List<Ocean> oceans;

    public boolean isValidTemperatute(List<ParameterColor> validParameters) {
        return isValidParameter(validParameters, GlobalParameter.TEMPERATURE);
    }

    public boolean isValidOxygen(List<ParameterColor> validParameters) {
        return isValidParameter(validParameters, GlobalParameter.OXYGEN);
    }

    public boolean isValidNumberOfOceans(LongPredicate oceanRequirement) {
        return oceanRequirement.test(oceans.stream().filter(Ocean::isRevealed).count());
    }

    public Optional<Ocean> revealOcean() {
        Optional<Ocean> result = oceans.stream().filter(ocean -> !ocean.isRevealed()).findAny();
        result.ifPresent(Ocean::reveal);
        return result;
    }

    private boolean isValidParameter(List<ParameterColor> validColors, GlobalParameter globalParameter) {
        if (validColors.isEmpty()) {
            return true;
        }

        return validColors.contains(measurableGlobalParameters.get(globalParameter).getCurrentColor());
    }
}
