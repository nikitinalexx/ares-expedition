package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.DecomposingFungus;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Component
@RequiredArgsConstructor
public class DecomposingFungusActionProcessor implements BlueActionCardProcessor<DecomposingFungus> {
    private final CardService cardService;

    @Override
    public Class<DecomposingFungus> getType() {
        return DecomposingFungus.class;
    }

    @Override
    public TurnResponse process(MarsGame game, PlayerContext player, List<Integer> inputParameters) {
        Integer cardIdToRemoveFrom = inputParameters.get(0);

        ProjectCard project = cardService.getProjectCard(cardIdToRemoveFrom);

        Integer currentResourceCount = player.getCardResourcesCount().get(project.getClass());

        player.getCardResourcesCount().put(project.getClass(), currentResourceCount - 1);

        player.setPlants(player.getPlants() + 3);

        return null;
    }
}
