package com.terraforming.ares.model.action;

import lombok.Builder;
import lombok.Data;

/**
 * Created by oleksii.nikitin
 * Creation date 15.05.2022
 */
@Data
@Builder
public class ActionInputData {
    private ActionInputDataType type;
    private int min;
    private int max;
}
