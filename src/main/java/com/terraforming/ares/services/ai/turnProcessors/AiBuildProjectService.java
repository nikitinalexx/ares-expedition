package com.terraforming.ares.services.ai.turnProcessors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.processors.turn.TurnProcessor;
import com.terraforming.ares.services.*;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.ProjectionStrategy;
import com.terraforming.ares.services.ai.RandomBotHelper;
import com.terraforming.ares.services.ai.dto.BuildProjectPrediction;
import com.terraforming.ares.services.ai.helpers.AiCardActionHelper;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 29.11.2022
 */
@Service
public class AiBuildProjectService extends BaseProcessorService {
    private final CardService cardService;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentService;
    private final AiCardBuildParamsHelper aiCardBuildParamsHelper;
    private final Random random = new Random();
    private final DeepNetwork deepNetwork;
    private final AiTurnService aiTurnService;
    private final StateFactory stateFactory;

    protected AiBuildProjectService(TurnTypeService turnTypeService,
                                    StateFactory stateFactory,
                                    StateContextProvider stateContextProvider,
                                    List<TurnProcessor<?>> turnProcessor,
                                    AiTurnService aiTurnService,
                                    AiPaymentService aiPaymentService,
                                    AiCardBuildParamsHelper aiCardBuildParamsHelper,
                                    CardValidationService cardValidationService,
                                    DeepNetwork deepNetwork, CardService cardService) {
        super(turnTypeService, stateFactory, stateContextProvider, turnProcessor);
        this.aiTurnService = aiTurnService;
        this.aiPaymentService = aiPaymentService;
        this.aiCardBuildParamsHelper = aiCardBuildParamsHelper;
        this.cardValidationService = cardValidationService;
        this.deepNetwork = deepNetwork;
        this.cardService = cardService;
        this.stateFactory = stateFactory;
    }

    public BuildProjectPrediction getBestProjectToBuild(MarsGame game, Player player, Set<CardColor> cardColors, ProjectionStrategy projectionStrategy) {
        List<Card> availableCards = player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> cardColors.contains(card.getColor()))
                .filter(card ->
                {
                    String errorMessage = cardValidationService.validateCard(
                            player, game, card.getId(),
                            aiPaymentService.getCardPayments(player, card),
                            aiCardBuildParamsHelper.getInputParamsForValidation(player, card)
                    );
                    return errorMessage == null;
                })
                .collect(Collectors.toList());

        if (availableCards.isEmpty()) {
            return BuildProjectPrediction.builder().canBuild(false).build();
        }

        Card selectedCard = null;
        float bestChance = RandomBotHelper.isRandomBot(player) ? 1.0f : deepNetwork.testState(game, player);
        if (RandomBotHelper.isRandomBot(player)) {
            selectedCard = availableCards.get(random.nextInt(availableCards.size()));
        } else {
            Card bestCard = null;
            for (Card playableCard : availableCards) {
                MarsGame stateAfterPlayingTheCard = projectBuildCard(game, player, playableCard, projectionStrategy);

                if (stateAfterPlayingTheCard == null) {
                    continue;
                }

                float projectedChance = deepNetwork.testState(stateAfterPlayingTheCard, stateAfterPlayingTheCard.getPlayerByUuid(player.getUuid()));

                if (projectedChance > bestChance) {
                    bestChance = projectedChance;
                    bestCard = playableCard;
                }
            }
            if (bestCard != null) {
                selectedCard = bestCard;
            }
        }

        if (selectedCard == null) {
            return BuildProjectPrediction.builder().canBuild(false).build();
        } else {
            return BuildProjectPrediction.builder().canBuild(true).expectedValue(bestChance).card(selectedCard).build();
        }
    }

    public MarsGame projectBuildCard(MarsGame game, Player player, Card card, ProjectionStrategy projectionStrategy) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        if (projectionStrategy == ProjectionStrategy.FROM_PICK_PHASE) {
            player.setPreviousChosenPhase(null);
            getAnotherPlayer(game, player).setPreviousChosenPhase(null);
            game.setStateType(StateType.PICK_PHASE, cardService);
            game.getPlayerUuidToPlayer().values().forEach(
                    p -> aiTurnService.choosePhaseTurn(p, card.getColor() == CardColor.GREEN ? 1 : 2)
            );
            while (processFinalTurns(game)) {
                stateFactory.getCurrentState(game).updateState();
            }
            String errorMessage = cardValidationService.validateCard(
                    player, game, card.getId(),
                    aiPaymentService.getCardPayments(player, card),
                    aiCardBuildParamsHelper.getInputParamsForValidation(player, card)
            );
            if (errorMessage != null) {
                return null;
            }
        }

        if (card.getColor() == CardColor.GREEN) {
            aiTurnService.buildGreenProjectSync(
                    game,
                    player,
                    card.getId(),
                    aiPaymentService.getCardPayments(player, card),
                    aiCardBuildParamsHelper.getInputParamsForBuild(game, player, card)
            );
        } else {
            aiTurnService.buildBlueRedProjectSync(
                    game,
                    player,
                    card.getId(),
                    aiPaymentService.getCardPayments(player, card),
                    aiCardBuildParamsHelper.getInputParamsForBuild(game, player, card)
            );
        }

        return game;
    }

    private Player getAnotherPlayer(MarsGame game, Player player) {
        return game.getPlayerUuidToPlayer().values().stream().filter(p -> !p.getUuid().equals(player.getUuid()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Another player not found"));
    }
}
