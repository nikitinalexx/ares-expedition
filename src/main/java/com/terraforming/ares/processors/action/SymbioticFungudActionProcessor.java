package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.SymbioticFungud;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class SymbioticFungudActionProcessor implements BlueActionCardProcessor<SymbioticFungud> {
    private final CardService cardService;

    @Override
    public Class<SymbioticFungud> getType() {
        return SymbioticFungud.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, List<Integer> inputParameters) {
        Integer cardIdToPutMicrobeOn = inputParameters.get(0);

        ProjectCard projectCard = cardService.getProjectCard(cardIdToPutMicrobeOn);

        player.getCardResourcesCount().put(
                projectCard.getClass(),
                player.getCardResourcesCount().get(projectCard.getClass()) + 1
        );

        return null;
    }

}
