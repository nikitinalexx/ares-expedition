package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.turn.GenericBuildProjectTurn;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;

import java.util.Collections;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@RequiredArgsConstructor
public abstract class GenericBuildProjectProcessor<T extends GenericBuildProjectTurn> implements TurnProcessor<T> {
    private final CardService cardService;

    @Override
    public TurnResponse processTurn(T turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());
        ProjectCard card = cardService.getProjectCard(turn.getProjectId());

        for (Payment payment : turn.getPayments()) {
            payment.pay(cardService, player);
        }

        for (Integer previouslyPlayedCardId : player.getPlayed().getCards()) {
            ProjectCard previouslyPlayedCard = cardService.getProjectCard(previouslyPlayedCardId);
            previouslyPlayedCard.onProjectBuiltEffect(game, player, card, turn.getInputParams());
        }

        player.getHand().removeCards(Collections.singletonList(turn.getProjectId()));
        player.getPlayed().addCard(turn.getProjectId());

        card.buildProject(player);

        processTurnInternal(turn, game);

        return null;
    }

    protected void processTurnInternal(T turn, MarsGame game) {}

}
