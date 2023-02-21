package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.MartianStudiesScholarship;
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
public class MartianStudiesScholarshipOnBuiltEffectValidator implements OnBuiltEffectValidator<MartianStudiesScholarship> {
    private final OnBuiltEffectValidationService onBuiltEffectValidationService;

    @Override
    public Class<MartianStudiesScholarship> getType() {
        return MartianStudiesScholarship.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        return onBuiltEffectValidationService.validatePhaseUpgrade(input);
    }
}
