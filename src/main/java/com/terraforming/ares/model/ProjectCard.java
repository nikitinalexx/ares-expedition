package com.terraforming.ares.model;

import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
public interface ProjectCard extends GenericCard {

    boolean isActiveCard();

    default CardCollectableResource getCollectableResource() {
        return CardCollectableResource.NONE;
    }

    default boolean onBuiltEffectApplicableToItself() {
        return false;
    }

    default boolean onBuiltEffectApplicableToOther() {
        return false;
    }

}
