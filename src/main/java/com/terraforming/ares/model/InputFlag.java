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
    VIRAL_ENHANCERS_PUT_RESOURCE(7),
    CEOS_FAVORITE_PUT_RESOURCES(8),
    IMPORTED_HYDROGEN_PICK_PLANT(9),
    IMPORTED_HYDROGEN_PUT_RESOURCE(10),
    IMPORTED_NITROGEN_ADD_ANIMALS(11),
    IMPORTED_NITROGEN_ADD_MICROBES(12),
    LARGE_CONVOY_PICK_PLANT(13),
    LARGE_CONVOY_ADD_ANIMAL(14),
    LOCAL_HEAT_TRAPPING_PUT_RESOURCE(15)
    ;

    private final int id;

    InputFlag(int id) {
        this.id = id;
    }
}
