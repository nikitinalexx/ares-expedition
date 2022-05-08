package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.BuildingIndustries;
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
public class BuildingIndustriesOnBuiltEffectValidator implements OnBuiltEffectValidator<BuildingIndustries> {

    @Override
    public Class<BuildingIndustries> getType() {
        return BuildingIndustries.class;
    }

    @Override
    public String validate(ProjectCard card, Player player, Map<Integer, List<Integer>> input) {
        if (player.getHeat() < 4) {
            return "Not enough Heat to build the project";
        }

        return null;
    }
}
