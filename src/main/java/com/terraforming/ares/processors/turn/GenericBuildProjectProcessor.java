package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.turn.GenericBuildProjectTurn;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;

import java.util.Collections;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@RequiredArgsConstructor
public abstract class GenericBuildProjectProcessor<T extends GenericBuildProjectTurn> implements TurnProcessor<T> {
    private final CardService cardService;
    private final TerraformingService terraformingService;

    @Override
    public TurnResponse processTurn(T turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());
        Card card = cardService.getCard(turn.getProjectId());

        for (Payment payment : turn.getPayments()) {
            payment.pay(cardService, player);
        }

        processInternalBeforeBuild(turn, game);

        TurnResponse response = card.buildProject(
                MarsContext.builder()
                        .game(game)
                        .player(player)
                        .terraformingService(terraformingService)
                        .cardService(cardService)
                        .build()
        );

        for (Integer previouslyPlayedCardId : player.getPlayed().getCards()) {
            Card previouslyPlayedCard = cardService.getCard(previouslyPlayedCardId);
            if (previouslyPlayedCard.onBuiltEffectApplicableToOther()) {
                previouslyPlayedCard.postProjectBuiltEffect(cardService, game, player, card, turn.getInputParams());
            }
        }

        player.getHand().removeCards(Collections.singletonList(turn.getProjectId()));
        player.getPlayed().addCard(turn.getProjectId());

        processInternalAfterBuild(turn, game);

        if (card.onBuiltEffectApplicableToItself()) {
            card.postProjectBuiltEffect(cardService, game, player, card, turn.getInputParams());
        }

        return response;
    }

    protected void processInternalAfterBuild(T turn, MarsGame game) {
    }

    protected void processInternalBeforeBuild(T turn, MarsGame game) {
    }

}
