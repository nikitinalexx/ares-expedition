package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.blue.SoftwareStreamlining;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class SoftwareStreamliningOnBuiltEffectValidator implements OnBuiltEffectValidator<SoftwareStreamlining> {
    private final OnBuiltEffectValidationService onBuiltEffectValidationService;


    @Override
    public Class<SoftwareStreamlining> getType() {
        return SoftwareStreamlining.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        return onBuiltEffectValidationService.validatePhaseUpgrade(input);
    }
}
