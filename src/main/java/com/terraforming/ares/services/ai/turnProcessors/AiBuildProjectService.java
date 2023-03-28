package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.dataset.MarsGameRow;
import com.terraforming.ares.dto.GameWithState;
import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.processors.turn.TurnProcessor;
import com.terraforming.ares.services.*;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.ProjectionStrategy;
import com.terraforming.ares.services.ai.RandomBotHelper;
import com.terraforming.ares.services.ai.dto.BuildProjectPrediction;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.terraforming.ares.model.Constants.*;

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
    private final MarsContextProvider contextProvider;
    private final WinPointsService winPointsService;
    private final DatasetCollectionService datasetCollectionService;

    protected AiBuildProjectService(TurnTypeService turnTypeService,
                                    StateFactory stateFactory,
                                    StateContextProvider stateContextProvider,
                                    List<TurnProcessor<?>> turnProcessor,
                                    AiTurnService aiTurnService,
                                    AiPaymentService aiPaymentService,
                                    AiCardBuildParamsHelper aiCardBuildParamsHelper,
                                    CardValidationService cardValidationService,
                                    DeepNetwork deepNetwork,
                                    CardService cardService,
                                    MarsContextProvider contextProvider,
                                    WinPointsService winPointsService,
                                    DatasetCollectionService datasetCollectionService) {
        super(turnTypeService, stateFactory, stateContextProvider, turnProcessor);
        this.aiTurnService = aiTurnService;
        this.aiPaymentService = aiPaymentService;
        this.aiCardBuildParamsHelper = aiCardBuildParamsHelper;
        this.cardValidationService = cardValidationService;
        this.deepNetwork = deepNetwork;
        this.cardService = cardService;
        this.stateFactory = stateFactory;
        this.contextProvider = contextProvider;
        this.winPointsService = winPointsService;
        this.datasetCollectionService = datasetCollectionService;
    }

    public BuildProjectPrediction getBestProjectToBuild(MarsGame game, Player player, Set<CardColor> cardColors, ProjectionStrategy projectionStrategy) {
        List<Card> availableCards = getHandAvailableCards(game, player, cardColors);

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
                GameWithState stateAfterPlayingTheCard = projectBuildCard(game, player, playableCard, projectionStrategy);

                if (stateAfterPlayingTheCard == null) {
                    continue;
                }

                float projectedChance = deepNetwork.testState(stateAfterPlayingTheCard.getGame(), stateAfterPlayingTheCard.getGame().getPlayerByUuid(player.getUuid()));

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

    public BuildProjectPrediction getBestProjectToBuildSecondPhase(MarsGame game, Player player, Set<CardColor> cardColors, ProjectionStrategy projectionStrategy) {
        List<Card> availableCards = getHandAvailableCards(game, player, cardColors);

        if (availableCards.isEmpty()) {
            return BuildProjectPrediction.builder().canBuild(false).build();
        }

        Card selectedCard = null;
        float bestChance = deepNetwork.testState(game, player);
        for (Card playableCard : availableCards) {
            GameWithState stateAfterPlayingTheCard = projectBuildCard(game, player, playableCard, projectionStrategy);

            if (stateAfterPlayingTheCard == null) {
                continue;
            }

            float projectedChance = deepNetwork.testState(stateAfterPlayingTheCard.getGame(), stateAfterPlayingTheCard.getGame().getPlayerByUuid(player.getUuid()));


            if (availableCards.size() > 1) {
                Float stateAfterPlayingSecondCard = projectSecondCardPlayed(stateAfterPlayingTheCard.getGame(), player.getUuid(), cardColors, projectionStrategy);

                if (stateAfterPlayingSecondCard != null && stateAfterPlayingSecondCard > projectedChance) {
                    projectedChance = stateAfterPlayingSecondCard;
                }
            } else {
                BuildProjectPrediction stateAfterTakingCard = projectTakeCardSecondPhase(stateAfterPlayingTheCard.getGame(), stateAfterPlayingTheCard.getGame().getPlayerByUuid(player.getUuid()));

                if (stateAfterTakingCard.isCanBuild() && stateAfterTakingCard.getExpectedValue() > projectedChance) {
                    projectedChance = stateAfterTakingCard.getExpectedValue();
                }
            }

            if (projectedChance > bestChance) {
                bestChance = projectedChance;
                selectedCard = playableCard;
            }
        }

        if (selectedCard == null) {
            return BuildProjectPrediction.builder().canBuild(false).build();
        } else {
            return BuildProjectPrediction.builder().canBuild(true).expectedValue(bestChance).card(selectedCard).build();
        }
    }

    private Float projectSecondCardPlayed(MarsGame game, String playerUuid, Set<CardColor> cardColors, ProjectionStrategy projectionStrategy) {
        Player player = game.getPlayerByUuid(playerUuid);

        List<Card> availableCards = getHandAvailableCards(game, player, cardColors);

        if (availableCards.isEmpty()) {
            return null;
        }

        float bestChance = deepNetwork.testState(game, player);

        for (Card playableCard : availableCards) {
            GameWithState stateAfterPlayingTheCard = projectBuildCard(game, player, playableCard, projectionStrategy);

            if (stateAfterPlayingTheCard == null) {
                continue;
            }

            float projectedChance = deepNetwork.testState(stateAfterPlayingTheCard.getGame(), stateAfterPlayingTheCard.getGame().getPlayerByUuid(player.getUuid()));

            if (projectedChance > bestChance) {
                bestChance = projectedChance;
            }
        }

        return bestChance;

    }

    private List<Card> getHandAvailableCards(MarsGame game, Player player, Set<CardColor> cardColors) {
        return player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> cardColors.contains(card.getColor()))
                .filter(card ->
                {
                    String errorMessage = cardValidationService.validateCard(
                            player, game, card.getId(),
                            aiPaymentService.getCardPayments(game, player, card),
                            aiCardBuildParamsHelper.getInputParamsForValidation(game, player, card)
                    );
                    return errorMessage == null;
                })
                .collect(Collectors.toList());
    }

    private BuildProjectPrediction projectTakeCardSecondPhase(MarsGame game, Player player) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        final MarsGameRow marsGameRow = datasetCollectionService.collectPlayerData(
                game,
                player,
                players.get(0) == player
                        ? players.get(1)
                        : players.get(0)
        );

        deepNetwork.testState(game, player);

        if (marsGameRow == null) {
            return BuildProjectPrediction.builder().canBuild(true).card(null).expectedValue(0.5f).build();
        }

        marsGameRow.setGreenCards(marsGameRow.getGreenCards() + GREEN_CARDS_RATIO);
        marsGameRow.setRedCards(marsGameRow.getRedCards() + RED_CARDS_RATIO);
        marsGameRow.setBlueCards(marsGameRow.getBlueCards() + BLUE_CARDS_RATIO);


        return BuildProjectPrediction.builder().canBuild(true).card(null).expectedValue(deepNetwork.testState(marsGameRow, player.isFirstBot() ? 1 : 2)).build();
    }

    public MarsGame projectBuildCardNoRequirements(MarsGame game, Player player, Card card) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        if (card.getColor() == CardColor.GREEN) {
            aiTurnService.buildGreenProjectSyncNoRequirements(
                    game,
                    player,
                    card.getId(),
                    aiPaymentService.getCardPayments(game, player, card),
                    aiCardBuildParamsHelper.getInputParamsForBuild(game, player, card)
            );
        } else {
            aiTurnService.buildBlueRedProjectSyncNoRequirements(
                    game,
                    player,
                    card.getId(),
                    aiPaymentService.getCardPayments(game, player, card),
                    aiCardBuildParamsHelper.getInputParamsForBuild(game, player, card)
            );
        }

        return game;
    }

    public GameWithState projectBuildCard(MarsGame game, Player player, Card card, ProjectionStrategy projectionStrategy) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());
        final MarsContext context = contextProvider.provide(game, player);

        if (projectionStrategy == ProjectionStrategy.FROM_PICK_PHASE) {
            player.setPreviousChosenPhase(null);
            getAnotherPlayer(game, player).setPreviousChosenPhase(null);
            game.setStateType(StateType.PICK_PHASE, context);
            game.getPlayerUuidToPlayer().values().forEach(
                    p -> aiTurnService.choosePhaseTurn(p, card.getColor() == CardColor.GREEN ? 1 : 2)
            );
            while (processFinalTurns(game)) {
                stateFactory.getCurrentState(game).updateState();
            }
            String errorMessage = cardValidationService.validateCard(
                    player, game, card.getId(),
                    aiPaymentService.getCardPayments(game, player, card),
                    aiCardBuildParamsHelper.getInputParamsForValidation(game, player, card)
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
                    aiPaymentService.getCardPayments(game, player, card),
                    aiCardBuildParamsHelper.getInputParamsForBuild(game, player, card)
            );
        } else {
            aiTurnService.buildBlueRedProjectSync(
                    game,
                    player,
                    card.getId(),
                    aiPaymentService.getCardPayments(game, player, card),
                    aiCardBuildParamsHelper.getInputParamsForBuild(game, player, card)
            );
        }


        GameWithState gameWithState = new GameWithState();
        gameWithState.setGame(game);

        return gameWithState;
    }

    private Player getAnotherPlayer(MarsGame game, Player player) {
        return game.getPlayerUuidToPlayer().values().stream().filter(p -> !p.getUuid().equals(player.getUuid()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Another player not found"));
    }
}
