package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.ResearchGrant;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@Component
public class ResearchGrantActionValidator implements ActionValidator<ResearchGrant> {
    @Override
    public Class<ResearchGrant> getType() {
        return ResearchGrant.class;
    }

    @Override
    public String validate(MarsGame game, Player player, List<Integer> inputParameters) {
        final List<Tag> cardTags = player.getCardToTag().get(ResearchGrant.class);
        if (cardTags.stream().noneMatch(tag -> tag == Tag.DYNAMIC)) {
            return "No more space for new tags";
        }
        if (inputParameters.isEmpty()) {
            return "Choose the tag to put on card";
        }

        final Tag tagInput = Tag.byIndex(inputParameters.get(0));

        if (tagInput == null || tagInput == Tag.DYNAMIC) {
            return "Choose the valid tag to put on card";
        }

        if (cardTags.stream().anyMatch(tag -> tag == tagInput)) {
            return "You can't put the same tag on this card";
        }
        return null;
    }
}
