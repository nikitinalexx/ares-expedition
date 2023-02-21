package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.ExperimentalTechnology;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.validation.input.OnBuiltEffectValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2023
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
    public String validate(MarsGame game, Player player, List<Integer> inputParameters) {
        return onBuiltEffectValidationService.validatePhaseUpgrade(inputParameters);
    }
}
