package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.AssetLiquidation;
import com.terraforming.ares.cards.blue.ExperimentalTechnology;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.validation.input.OnBuiltEffectValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Component
@RequiredArgsConstructor
public class ExperimentalTechnologyActionValidator implements ActionValidator<ExperimentalTechnology> {
    private final OnBuiltEffectValidationService onBuiltEffectValidationService;

    @Override
    public Class<ExperimentalTechnology> getType() {
        return ExperimentalTechnology.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        if (player.getTerraformingRating() < 1) {
            return "Not enough TR to upgrade a phase";
        }

        return onBuiltEffectValidationService.validatePhaseUpgrade(inputParameters);
    }

}
