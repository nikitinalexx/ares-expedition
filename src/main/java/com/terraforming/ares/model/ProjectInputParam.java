package com.terraforming.ares.model;

import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Getter
public enum ProjectInputParam {
    DECOMPOSERS_TAKE_MICROBE(1),
    DECOMPOSERS_TAKE_CARD(2),
    EXTEME_COLD_FUNGUS_PICK_PLANT(3),
    EXTREME_COLD_FUNGUS_PUT_MICROBE(4);

    private final int id;

    ProjectInputParam(int id) {
        this.id = id;
    }
}
