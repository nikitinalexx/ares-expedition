package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.VirtualEmployeeDevelopment;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.validation.input.OnBuiltEffectValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2023
 */
@Component
@RequiredArgsConstructor
public class VirtualEmployeeDevelopmentActionValidator implements ActionValidator<VirtualEmployeeDevelopment> {
    private final OnBuiltEffectValidationService onBuiltEffectValidationService;

    @Override
    public Class<VirtualEmployeeDevelopment> getType() {
        return VirtualEmployeeDevelopment.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        return onBuiltEffectValidationService.validatePhaseUpgrade(inputParameters);
    }
}
