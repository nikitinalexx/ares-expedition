package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.DecomposingFungus;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;
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
public class DecomposingFungusActionValidator implements ActionValidator<DecomposingFungus> {
    private final CardService cardService;

    @Override
    public Class<DecomposingFungus> getType() {
        return DecomposingFungus.class;
    }

    @Override
    public String validate(Planet planet, Player player, List<Integer> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters) || inputParameters.size() != 1) {
            return "Decomposing fungus expects a card to remove an Animal/Microbe from";
        }

        Integer cardToRemoveFrom = inputParameters.get(0);

        if (!player.getPlayed().containsCard(cardToRemoveFrom)) {
            return "Player doesn't have the selected project card.";
        }

        ProjectCard project = cardService.getProjectCard(cardToRemoveFrom);
        CardCollectableResource collectableResourceType = project.getCollectableResource();

        if (collectableResourceType != CardCollectableResource.MICROBE && collectableResourceType != CardCollectableResource.ANIMAL) {
            return "You may only remove resource from an Animal/Microbe card";
        }

        if (player.getCardResourcesCount().get(project.getClass()) < 1) {
            return "The selected card doesn't have enough resources to perform current action";
        }

        return null;
    }
}
