package com.terraforming.ares.model;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
public interface ProjectCard extends GenericCard {

    default boolean onBuiltEffectApplicableToItself() {
        return false;
    }

    default boolean onBuiltEffectApplicableToOther() {
        return false;
    }

}
