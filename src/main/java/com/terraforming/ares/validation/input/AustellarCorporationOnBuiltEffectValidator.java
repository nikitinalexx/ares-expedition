package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.corporations.ApolloIndustriesCorporation;
import com.terraforming.ares.cards.corporations.AustellarCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.milestones.Milestone;
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
public class AustellarCorporationOnBuiltEffectValidator implements OnBuiltEffectValidator<AustellarCorporation> {
    private static final String INCORRECT_TAG_INPUT_ERROR_MESSAGE =
            "Austellar: requires an input with the tag";

    private static final String INCORRECT_MILESTONE_INPUT_ERROR_MESSAGE =
            "Austellar: requires an input with the milestone";

    @Override
    public Class<AustellarCorporation> getType() {
        return AustellarCorporation.class;
    }

    @Override
    public String validate(MarsGame game, Card card, Player player, Map<Integer, List<Integer>> input) {
        List<Integer> tagInput = input.get(InputFlag.TAG_INPUT.getId());

        if (CollectionUtils.isEmpty(tagInput)) {
            return INCORRECT_TAG_INPUT_ERROR_MESSAGE;
        }

        final Tag tag = Tag.byIndex(tagInput.get(0));

        if (tag == null || tag == Tag.DYNAMIC) {
            return INCORRECT_TAG_INPUT_ERROR_MESSAGE;
        }

        final List<Integer> milestoneInput = input.get(InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId());
        if (CollectionUtils.isEmpty(milestoneInput)) {
            return INCORRECT_MILESTONE_INPUT_ERROR_MESSAGE;
        }

        int milestone = milestoneInput.get(0);
        final List<Milestone> milestones = game.getMilestones();

        if (milestone < 0 || milestone >= milestones.size()) {
            return INCORRECT_MILESTONE_INPUT_ERROR_MESSAGE;
        }

        return null;
    }
}
