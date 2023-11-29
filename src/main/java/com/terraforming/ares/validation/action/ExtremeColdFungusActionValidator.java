package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.cards.blue.ExtremeColdFungus;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardResourceService;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class ExtremeColdFungusActionValidator implements ActionValidator<ExtremeColdFungus> {
    private final CardService cardService;
    private final CardResourceService cardResourceService;

    @Override
    public Class<ExtremeColdFungus> getType() {
        return ExtremeColdFungus.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return "Extreme cold fungus expects a choice to get a plant or put a microbe";
        }

        final List<Integer> pickPlantInput = inputParameters.get(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId());
        final List<Integer> putMicrobeInput = inputParameters.get(InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId());

        if (CollectionUtils.isEmpty(pickPlantInput)
                && CollectionUtils.isEmpty(putMicrobeInput)
                || !CollectionUtils.isEmpty(pickPlantInput)
                && !CollectionUtils.isEmpty(putMicrobeInput)) {
            return "Invalid input for Extreme cold fungus, only PickPlant/PutMicrobe supported";
        }

        if (!CollectionUtils.isEmpty(putMicrobeInput)) {
            Integer cardToPutOn = putMicrobeInput.get(0);

            if (!player.getPlayed().containsCard(cardToPutOn)) {
                return "Player doesn't have the selected project card.";
            }

            Card project = cardService.getCard(cardToPutOn);

            if (project.getCollectableResource() != CardCollectableResource.MICROBE) {
                return "You may only add resource to a Microbe card";
            }
            String resourceSubmissionMessage = cardResourceService.resourceSubmissionMessage(project,player);
            if (resourceSubmissionMessage != null) {
                return resourceSubmissionMessage;
            }
        }

        return null;
    }
}
