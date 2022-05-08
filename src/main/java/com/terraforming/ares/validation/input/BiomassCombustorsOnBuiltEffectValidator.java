package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.BiomassCombustors;
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
public class BiomassCombustorsOnBuiltEffectValidator implements OnBuiltEffectValidator<BiomassCombustors> {

    @Override
    public Class<BiomassCombustors> getType() {
        return BiomassCombustors.class;
    }

    @Override
    public String validate(ProjectCard card, Player player, Map<Integer, List<Integer>> input) {
        if (player.getPlants() < 2) {
            return "Not enough Plants to build the project";
        }

        return null;
    }
}
