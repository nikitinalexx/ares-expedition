package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.Biofoundries;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 20.02.2023
 */
@Component
@RequiredArgsConstructor
public class BiofoundriesOnBuiltEffectValidator implements OnBuiltEffectValidator<Biofoundries> {
    private final OnBuiltEffectValidationService onBuiltEffectValidationService;

    @Override
    public Class<Biofoundries> getType() {
        return Biofoundries.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        return onBuiltEffectValidationService.validatePhaseUpgrade(input);
    }
}