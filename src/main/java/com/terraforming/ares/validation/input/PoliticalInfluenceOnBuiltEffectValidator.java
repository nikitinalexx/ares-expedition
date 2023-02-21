package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.PoliticalInfluence;
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
 * Creation date 19.02.2023
 */
@Component
@RequiredArgsConstructor
public class PoliticalInfluenceOnBuiltEffectValidator implements OnBuiltEffectValidator<PoliticalInfluence> {
    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "Political Influence: requires an input with the tag";

    @Override
    public Class<PoliticalInfluence> getType() {
        return PoliticalInfluence.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        List<Integer> tagInput = input.get(InputFlag.TAG_INPUT.getId());

        if (CollectionUtils.isEmpty(tagInput)) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        final Tag tag = Tag.byIndex(tagInput.get(0));

        if (tag == null || tag == Tag.DYNAMIC) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        return null;
    }
}
