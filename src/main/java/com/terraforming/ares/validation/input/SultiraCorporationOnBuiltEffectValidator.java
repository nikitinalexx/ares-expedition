package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.corporations.SultiraCorporation;
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
public class SultiraCorporationOnBuiltEffectValidator implements OnBuiltEffectValidator<SultiraCorporation> {
    private final OnBuiltEffectValidationService onBuiltEffectValidationService;

    @Override
    public Class<SultiraCorporation> getType() {
        return SultiraCorporation.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        if (card.getClass() != SultiraCorporation.class) {
            return null;
        }

        return onBuiltEffectValidationService.validatePhaseUpgrade(input, 1);
    }
}
