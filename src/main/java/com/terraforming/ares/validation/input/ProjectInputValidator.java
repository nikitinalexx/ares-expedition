package com.terraforming.ares.validation.input;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;

import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
public interface ProjectInputValidator {

    String validate(ProjectCard card, Player player, Map<Integer, Integer> input);

}
