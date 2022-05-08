package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.Moss;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class MossOnBuiltEffectValidator implements OnBuiltEffectValidator<Moss> {

    @Override
    public Class<Moss> getType() {
        return Moss.class;
    }

    @Override
    public String validate(ProjectCard card, Player player, Map<Integer, List<Integer>> input) {
        if (player.getPlants() < 1) {
            return "Not enough Plants to build the project";
        }

        return null;
    }
}
