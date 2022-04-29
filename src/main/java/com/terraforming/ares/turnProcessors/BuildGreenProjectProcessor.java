package com.terraforming.ares.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.turn.BuildGreenProjectTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.DeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class BuildGreenProjectProcessor implements TurnProcessor<BuildGreenProjectTurn> {
    private final DeckService marsDeckService;

    @Override
    public void processTurn(BuildGreenProjectTurn turn, MarsGame game) {
        PlayerContext playerContext = game.getPlayerContexts().get(turn.getPlayerUuid());

        playerContext.getHand().removeCards(Collections.singletonList(turn.getProjectId()));
        playerContext.getPlayed().addCard(turn.getProjectId());

        ProjectCard card = marsDeckService.getProjectCard(turn.getProjectId());
        card.buildProject(playerContext);
    }

    @Override
    public TurnType getType() {
        return TurnType.BUILD_GREEN_PROJECT;
    }

}
