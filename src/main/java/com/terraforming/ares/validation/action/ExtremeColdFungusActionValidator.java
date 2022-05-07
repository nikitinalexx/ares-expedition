package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.ExtremeColdFungus;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class ExtremeColdFungusActionValidator implements ActionValidator<ExtremeColdFungus> {
    private final CardService cardService;

    @Override
    public Class<ExtremeColdFungus> getType() {
        return ExtremeColdFungus.class;
    }

    @Override
    public String validate(Planet planet, Player player, List<Integer> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return "Extreme cold fungus expects a choice to get a plant or put a microbe";
        }

        Integer choiceType = inputParameters.get(0);

        if (choiceType != InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId()
                && choiceType != InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId()) {
            return "Invalid input for Extreme cold fungus, only PickPlant/PutMicrobe supported";
        }

        if (choiceType == InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId() && inputParameters.size() < 2) {
            return "Invalid input for Extreme cold fungus Put Microbe action, card to put on not provided";
        }

        if (choiceType == InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId()) {
            Integer cardToPutOn = inputParameters.get(1);

            if (!player.getPlayed().containsCard(cardToPutOn)) {
                return "Player doesn't have the selected project card.";
            }

            ProjectCard project = cardService.getProjectCard(cardToPutOn);

            if (project.getCollectableResource() != CardCollectableResource.MICROBE) {
                return "You may only add resource to a Microbe card";
            }
        }

        return null;
    }
}
