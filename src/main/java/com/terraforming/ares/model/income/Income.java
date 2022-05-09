package com.terraforming.ares.model.income;

import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 09.05.2022
 */
@Value
public class Income {
    IncomeType type;
    int value;

    public static Income of(IncomeType type, int value) {
        return new Income(type, value);
    }

}
