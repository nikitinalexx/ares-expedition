package com.terraforming.ares.validation.input;

import com.terraforming.ares.model.GenericCard;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
public interface OnBuiltEffectValidator<T extends GenericCard> {

    Class<T> getType();

    String validate(ProjectCard card, Player player, Map<Integer, List<Integer>> input);

}
