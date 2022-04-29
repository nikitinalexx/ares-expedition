package com.terraforming.ares.model;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
public interface GenericCard {

    void buildProject(PlayerContext playerContext);

    //TODO find a better way to transfer card description and parameters
    String description();

    List<Tag> getTags();

    Expansion getExpansion();

}
