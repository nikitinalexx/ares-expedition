package com.alex.nikitin.terraformingm.model;

import com.alex.nikitin.terraformingm.model.parameters.MeasurableGlobalParameter;
import com.alex.nikitin.terraformingm.model.parameters.Ocean;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
public class Planet {
    private final Map<GlobalParameter, MeasurableGlobalParameter> measurableGlobalParameters;
    private final List<Ocean> oceans;
}
