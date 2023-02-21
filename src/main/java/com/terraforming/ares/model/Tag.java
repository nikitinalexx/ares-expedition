package com.terraforming.ares.model;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
public enum Tag {
    SPACE, EARTH, EVENT, SCIENCE, PLANT, ENERGY, BUILDING, ANIMAL, JUPITER, MICROBE, DYNAMIC;

    public static Tag byIndex(int index) {
        final Tag[] values = Tag.values();
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }

}
