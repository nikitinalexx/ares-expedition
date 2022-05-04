package com.terraforming.ares.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.turn.GenericBuildProjectTurn;
import com.terraforming.ares.services.DeckService;
import lombok.RequiredArgsConstructor;

import java.util.Collections;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@RequiredArgsConstructor
public abstract class GenericBuildProjectProcessor<T extends GenericBuildProjectTurn> implements TurnProcessor<T> {
    private final DeckService marsDeckService;

    @Override
    public void processTurn(T turn, MarsGame game) {
        PlayerContext playerContext = game.getPlayerContexts().get(turn.getPlayerUuid());

        for (Payment payment : turn.getPayments()) {
            payment.pay(playerContext);
        }

        playerContext.getHand().removeCards(Collections.singletonList(turn.getProjectId()));
        playerContext.getPlayed().addCard(turn.getProjectId());

        ProjectCard card = marsDeckService.getProjectCard(turn.getProjectId());
        card.buildProject(playerContext);
    }

}
