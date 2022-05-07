package com.terraforming.ares.model;

import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Getter
public enum InputFlag {
    SKIP_ACTION(-1),
    DECOMPOSERS_TAKE_MICROBE(1),
    DECOMPOSERS_TAKE_CARD(2),
    EXTEME_COLD_FUNGUS_PICK_PLANT(3),
    EXTREME_COLD_FUNGUS_PUT_MICROBE(4),
    MARS_UNIVERSITY_CARD(5),
    VIRAL_ENHANCERS_TAKE_PLANT(6),
    VIRAL_ENHANCERS_PUT_RESOURCE(7);

    private final int id;

    InputFlag(int id) {
        this.id = id;
    }
}
