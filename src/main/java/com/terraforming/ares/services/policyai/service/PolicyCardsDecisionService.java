package com.terraforming.ares.services.policyai.service;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.SellCardsLastRoundTurn;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.policyai.action.ActionHead;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.encoder.PolicyTableAndHandEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class PolicyCardsDecisionService {
    private final PolicyTableAndHandEncoder encoder;
    private final CardService cardService;
    private final PolicyActionSelector actionSelector;

    private HeadAction predict(PolicyRecord record, List<HeadAction> validActions) {
        return actionSelector.chooseBestAction(record, validActions, null);
    }

    public List<Integer> mulliganCards(MarsGame game, Player currentPlayer) {
        if (!AiConstants.POLICY_PLAYING) {
            return List.of();
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == currentPlayer ? players.get(1) : players.get(0);

        PolicyRecord record = new PolicyRecord();
        encoder.encode(game, currentPlayer, anotherPlayer, record);
        record.initialSetup = true;
        record.doingMulligan = true;
        record.mulliganToDraw = 0;

        Map<HeadAction, Card> actionToCard = new HashMap<>();
        List<HeadAction> validActions = new ArrayList<>();
        for (Integer cardId : currentPlayer.getHand().getCards()) {
            Card card = cardService.getCard(cardId);
            HeadAction action = ActionInputService.sellDiscardCardAction(card);
            validActions.add(action);
            actionToCard.put(action, card);
        }

        validActions.add(ActionInputService.passMulligan());

        List<Integer> cardsToDiscard = new ArrayList<>();

        while (validActions.size() > 1) {
            HeadAction nextAction = predict(record, validActions);

            if (nextAction.head() == ActionHead.SELL_DISCARD) {

                Card cardToDiscard = actionToCard.get(nextAction);

                record.mulliganToDraw++;
                record.removeCardFromHand(cardToDiscard);
                cardsToDiscard.add(cardToDiscard.getId());

                validActions.remove(nextAction);
            } else {
                //we reached pass
                break;
            }
        }

        return cardsToDiscard;
    }

    public List<Integer> discardingCardsFromHand(
            MarsGame game,
            Player player,
            int cardsToDiscardCount,
            boolean isPostBuild
    ) {

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord record = new PolicyRecord();
        encoder.encode(game, player, anotherPlayer, record);

        record.cardsLeftToDiscard = (byte) cardsToDiscardCount;

        if (isPostBuild) {
            record.postBuildDiscarding = true;
            if (anotherPlayer.getNextTurn() instanceof DiscardCardsTurn opponentTurn) {
                record.opponentLeftToDiscard = (byte) opponentTurn.getSize();
            }
        } else {
            record.discardingLastTurn = true;
            if (anotherPlayer.getNextTurn() instanceof SellCardsLastRoundTurn opponentTurn) {
                record.opponentLeftToDiscard = (byte) opponentTurn.getCards().size();
            } else if (anotherPlayer.getHand().size() > 10) {
                record.opponentLeftToDiscard = (byte) (anotherPlayer.getHand().size() - 10);
            }
        }

        Map<HeadAction, Card> actionToCard = new HashMap<>();
        List<HeadAction> validActions = new ArrayList<>();

        for (Integer cardId : player.getHand().getCards()) {
            Card card = cardService.getCard(cardId);
            HeadAction action = ActionInputService.sellDiscardCardAction(card);
            validActions.add(action);
            actionToCard.put(action, card);
        }

        List<Integer> cardsToDiscard = new ArrayList<>();

        while (record.cardsLeftToDiscard > 0 && validActions.size() > 1) {

            HeadAction chosen = predict(record, validActions);


            Card card = actionToCard.get(chosen);

            cardsToDiscard.add(card.getId());

            record.removeCardFromHand(card);
            record.cardsLeftToDiscard--;

            validActions.remove(chosen);
        }

        if (record.cardsLeftToDiscard > 0 && validActions.size() == 1) {
            cardsToDiscard.add(actionToCard.get(validActions.getFirst()).getId());
        }

        return cardsToDiscard;
    }

    public List<Integer> discardingFromSelected(
            MarsGame game,
            Player player,
            List<Integer> discardingFrom,
            int cardsToDiscardCount
    ) {
        if (!AiConstants.POLICY_PLAYING) {
            return List.of();
        }

        if (cardsToDiscardCount == discardingFrom.size()) {
            return new ArrayList<>(discardingFrom);
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord record = new PolicyRecord();
        encoder.encode(game, player, anotherPlayer, record);
        record.cardsLeftToDiscard = (byte) cardsToDiscardCount;
        record.discardingFromSelected = true;
        if (anotherPlayer.getNextTurn() instanceof DiscardCardsTurn opponentTurn) {
            record.opponentLeftToDiscard = (byte) opponentTurn.getSize();
        }

        for (Integer cardId : discardingFrom) {
            record.selectCard(cardService.getCard(cardId));
        }

        Map<HeadAction, Card> actionToCard = new HashMap<>();
        List<HeadAction> validActions = new ArrayList<>();

        for (Integer cardId : discardingFrom) {
            Card card = cardService.getCard(cardId);
            HeadAction action = ActionInputService.sellDiscardCardAction(card);
            validActions.add(action);
            actionToCard.put(action, card);
        }

        List<Integer> cardsToDiscard = new ArrayList<>();

        while (record.cardsLeftToDiscard > 0 && validActions.size() > 1) {

            HeadAction chosen = predict(record, validActions);

            Card card = actionToCard.get(chosen);

            cardsToDiscard.add(card.getId());

            record.removeCardFromHand(card);
            record.removeCardFromSelected(card);
            record.cardsLeftToDiscard--;

            validActions.remove(chosen);
        }

        if (record.cardsLeftToDiscard > 0 && validActions.size() == 1) {
            cardsToDiscard.add(actionToCard.get(validActions.getFirst()).getId());
        }

        return cardsToDiscard;
    }

}
