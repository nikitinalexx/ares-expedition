package com.terraforming.ares.model;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.03.2023
 */
public enum CrisisDummyCard {
    SOLO_3_4,
    SOLO_5_3,
    SOLO_4_3,
    SOLO_4_2,
    SOLO_4_1;

    public static final List<CrisisDummyCard> ALL_SOLO_DUMMY_CARDS = List.of(
            SOLO_3_4, SOLO_5_3, SOLO_4_3, SOLO_4_2, SOLO_4_1
    );

    public boolean validCardPattern(String pattern) {
        if (pattern == null || pattern.length() != 3) {
            return false;
        }
        String firstPattern = "SOLO_" + pattern.charAt(0) + "_" + pattern.charAt(2);
        String secondPattern = "SOLO_" + pattern.charAt(2) + "_" + pattern.charAt(0);

        return name().equals(firstPattern) || name().equals(secondPattern);
    }
}
