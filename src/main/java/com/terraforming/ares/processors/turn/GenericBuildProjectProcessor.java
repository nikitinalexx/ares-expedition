package com.terraforming.ares.processors.turn;

import com.terraforming.ares.cards.corporations.DummyCard;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.turn.GenericBuildProjectTurn;
import com.terraforming.ares.services.BuildService;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DiscountService;
import com.terraforming.ares.services.MarsContextProvider;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@RequiredArgsConstructor
public abstract class GenericBuildProjectProcessor<T extends GenericBuildProjectTurn> implements TurnProcessor<T> {
    protected final CardService cardService;
    private final BuildService buildService;
    private final DiscountService discountService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public TurnResponse processTurn(T turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());
        Card card = cardService.getCard(turn.getProjectId());

        for (Payment payment : turn.getPayments()) {
            payment.pay(cardService, player);
        }

        player.setBuiltSpecialDesignLastTurn(false);

        int discount = discountService.getDiscount(game, card, player, turn.getInputParams());
        discount += turn.getPayments().stream().mapToInt(Payment::getDiscount).sum();

        final BuildDto buildOption = buildService.findMostOptimalBuild(card, player, discount);

        final MarsContext marsContext = marsContextProvider.provide(game, player, turn.getInputParams());

        TurnResponse response = card.buildProject(marsContext);

        for (Integer previouslyPlayedCardId : player.getPlayed().getCards()) {
            Card previouslyPlayedCard = cardService.getCard(previouslyPlayedCardId);
            if (previouslyPlayedCard.onBuiltEffectApplicableToOther()) {
                previouslyPlayedCard.postProjectBuiltEffect(marsContext, card, turn.getInputParams());
            }
        }

        for (Player anotherPlayer : game.getPlayerUuidToPlayer().values()) {
            anotherPlayer.getPlayed().getCards().stream()
                    .map(cardService::getCard)
                    .filter(Card::onBuiltEffectApplicableToAllPlayers)
                    .forEach(
                            c -> c.postProjectBuiltEffectForAll(marsContext, card, anotherPlayer, turn.getInputParams())
                    );
        }

        if (game.isCrysis()) {
            new ArrayList<>(game.getCrysisData()
                    .getOpenedCards())
                    .stream()
                    .map(cardService::getCrysisCard)
                    .forEach(c -> c.postProjectBuiltEffect(marsContext, card, turn.getInputParams()));
        }

        player.getHand().removeCards(Collections.singletonList(turn.getProjectId()));
        player.getPlayed().addCard(turn.getProjectId(), game.getTurns());

        if (card.onBuiltEffectApplicableToItself()) {
            card.postProjectBuiltEffect(marsContext, card, turn.getInputParams());
        }

        player.getBuilds().remove(buildOption);

        return response;
    }

}
