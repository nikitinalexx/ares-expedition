package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.TopographicMapping;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 20.02.2023
 */
@Component
@RequiredArgsConstructor
public class TopographicMappingOnBuiltEffectValidator implements OnBuiltEffectValidator<TopographicMapping> {
    private final OnBuiltEffectValidationService onBuiltEffectValidationService;

    private static final String INCORRECT_TAG_INPUT_ERROR_MESSAGE =
            "Topographic Mapping: requires an input with the tag";

    @Override
    public Class<TopographicMapping> getType() {
        return TopographicMapping.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        final String errorMessage = onBuiltEffectValidationService.validatePhaseUpgrade(input);
        if (errorMessage != null) {
            return errorMessage;
        }

        List<Integer> tagInput = input.get(InputFlag.TAG_INPUT.getId());

        if (CollectionUtils.isEmpty(tagInput)) {
            return INCORRECT_TAG_INPUT_ERROR_MESSAGE;
        }

        final Tag tag = Tag.byIndex(tagInput.get(0));

        if (tag == null || tag == Tag.DYNAMIC) {
            return INCORRECT_TAG_INPUT_ERROR_MESSAGE;
        }

        return null;
    }
}
