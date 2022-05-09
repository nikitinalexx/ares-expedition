package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.ConservedBiome;
import com.terraforming.ares.mars.MarsGame;
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
 * Creation date 06.05.2022
 */
@Component
@RequiredArgsConstructor
public class ConservedBiomeActionValidator implements ActionValidator<ConservedBiome> {
    private final CardService cardService;

    @Override
    public Class<ConservedBiome> getType() {
        return ConservedBiome.class;
    }

    @Override
    public String validate(MarsGame game, Player player, List<Integer> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters) || inputParameters.size() != 1) {
            return "Conserved biome expects a card to add an Animal/Microbe to";
        }

        Integer cardToAddTo = inputParameters.get(0);

        if (!player.getPlayed().containsCard(cardToAddTo)) {
            return "Player doesn't have the selected project card.";
        }

        ProjectCard project = cardService.getProjectCard(cardToAddTo);
        CardCollectableResource collectableResourceType = project.getCollectableResource();

        if (collectableResourceType != CardCollectableResource.MICROBE && collectableResourceType != CardCollectableResource.ANIMAL) {
            return "You may only add resource to an Animal/Microbe card";
        }

        return null;
    }
}
