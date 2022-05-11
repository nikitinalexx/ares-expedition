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
    CardColor getColor();

    default List<Tag> getTagRequirements() {
        return Collections.emptyList();
    }

    default List<ParameterColor> getTemperatureRequirement() {
        return Collections.emptyList();
    }

    default List<ParameterColor> getOxygenRequirement() {
        return Collections.emptyList();
    }

    default OceanRequirement getOceanRequirement() {
        return null;
    }

    boolean isActiveCard();

    default int getWinningPoints() {
        return 0;
    }

    default Set<SpecialEffect> getSpecialEffects() {
        return Collections.emptySet();
    }

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
