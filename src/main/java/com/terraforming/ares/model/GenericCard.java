package com.terraforming.ares.model;

import com.terraforming.ares.mars.MarsGame;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
public interface GenericCard {

    int getId();

    default void buildProject(PlayerContext playerContext) {
    }

    //TODO find a better way to transfer card description and parameters
    String description();

    default List<Tag> getTags() {
        return Collections.emptyList();
    }

    ;

    Expansion getExpansion();

    default void onProjectBuiltEffect(MarsGame game, PlayerContext player, ProjectCard project, Map<Integer, Integer> inputParams) {
    }

    default void onOceanFlippedEffect(PlayerContext player) {
    }

}
