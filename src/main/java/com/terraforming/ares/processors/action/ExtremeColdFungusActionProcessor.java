package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.ExtremeColdFungus;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.ProjectInputParam;
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
public class ExtremeColdFungusActionProcessor implements BlueActionCardProcessor<ExtremeColdFungus> {
    private final CardService cardService;

    @Override
    public Class<ExtremeColdFungus> getType() {
        return ExtremeColdFungus.class;
    }

    @Override
    public TurnResponse process(MarsGame game, PlayerContext player, List<Integer> inputParameters) {
        Integer choiceType = inputParameters.get(0);

        if (choiceType == ProjectInputParam.EXTEME_COLD_FUNGUS_PICK_PLANT.getId()) {
            player.setPlants(player.getPlants() + 1);
        } else {
            Integer cardToAddTo = inputParameters.get(1);

            ProjectCard project = cardService.getProjectCard(cardToAddTo);

            Integer currentResourceCount = player.getCardResourcesCount().get(project.getClass());

            player.getCardResourcesCount().put(project.getClass(), currentResourceCount + 1);
        }

        return null;
    }
}
