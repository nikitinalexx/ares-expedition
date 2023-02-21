package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.StandardProjectType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 18.05.2022
 */
@Service
@RequiredArgsConstructor
public class StandardProjectService {
    private final SpecialEffectsService specialEffectsService;
    private final TerraformingService terraformingService;

    private static final Map<StandardProjectType, Integer> PROJECT_TO_PRICE =
            Map.of(
                    StandardProjectType.FOREST, Constants.FOREST_MC_COST,
                    StandardProjectType.TEMPERATURE, Constants.TEMPERATURE_MC_COST,
                    StandardProjectType.OCEAN, Constants.OCEAN_MC_COST
            );

    public String validateStandardProject(MarsGame game, Player player, StandardProjectType type) {
        if (type == StandardProjectType.OCEAN && !terraformingService.canRevealOcean(game)) {
            return "Can't reveal oceans anymore";
        }
        if (type == StandardProjectType.TEMPERATURE && !terraformingService.canIncreaseTemperature(game)) {
            return "Can't increase temperature anymore";
        }

        int price = getProjectPrice(player, type);

        if (player.getMc() < price) {
            return "Not enough MC to make a standard project";
        }

        return null;
    }

    public String validateStandardProjectAvailability(MarsGame game, Player player) {
        if (terraformingService.canRevealOcean(game) && getProjectPrice(player, StandardProjectType.OCEAN) <= player.getMc()) {
            return "Standard action: Ocean is available";
        }

        if (terraformingService.canIncreaseTemperature(game) && getProjectPrice(player, StandardProjectType.TEMPERATURE) <= player.getMc()) {
            return "Standard action: Temperature is available";
        }

        if (getProjectPrice(player, StandardProjectType.FOREST) <= player.getMc()) {
            return "Standard action: Forest is available";
        }

        return null;
    }

    public int getProjectPrice(Player player, StandardProjectType type) {
        int price = PROJECT_TO_PRICE.get(type);

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.STANDARD_TECHNOLOGY)) {
            price -= 4;
        }

        return price;
    }

}
