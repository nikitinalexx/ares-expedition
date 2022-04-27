package com.terraforming.ares.model;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
public interface CorporationCard {

    void buildProject(PlayerContext playerContext);

    //TODO find a better way to transfer card description and parameters
    String description();

    Set<Tag> getTags();

    Expansion getExpansion();
}
