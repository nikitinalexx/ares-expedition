package com.terraforming.ares.validation.project;

import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;

import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
public interface ProjectInputValidator {

    String validate(ProjectCard card, PlayerContext player, Map<Integer, Integer> input);

}
