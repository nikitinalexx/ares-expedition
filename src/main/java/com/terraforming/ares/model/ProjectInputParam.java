package com.terraforming.ares.model;

import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Getter
public enum ProjectInputParam {
    DECOMPOSERS_TAKE_MICROBE(1),
    DECOMPOSERS_TAKE_CARD(2);

    private final int id;

    ProjectInputParam(int id) {
        this.id = id;
    }
}
