package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.dataset.MarsGameRow;
import com.terraforming.ares.dataset.MarsPlayerRow;
import com.terraforming.ares.dto.GameWithState;
import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.processors.turn.TurnProcessor;
import com.terraforming.ares.services.*;
import com.terraforming.ares.services.ai.AiCardValidationService;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.Phase;
import com.terraforming.ares.services.ai.ProjectionStrategy;
import com.terraforming.ares.services.ai.dto.BuildProjectPrediction;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsService;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private final AiCardBuildParamsService aiCardBuildParamsService;
    private final Random random = new Random();
    private final DeepNetwork deepNetwork;
    private final AiTurnService aiTurnService;
    private final StateFactory stateFactory;
    private final MarsContextProvider contextProvider;
    private final AiCardValidationService aiCardValidationService;
    private final DatasetCollectionService datasetCollectionService;

    protected AiBuildProjectService(TurnTypeService turnTypeService,
                                    StateFactory stateFactory,
                                    StateContextProvider stateContextProvider,
                                    List<TurnProcessor<?>> turnProcessor,
                                    AiTurnService aiTurnService,
                                    AiPaymentService aiPaymentService,
                                    AiCardBuildParamsService aiCardBuildParamsService,
                                    CardValidationService cardValidationService,
                                    DeepNetwork deepNetwork,
                                    CardService cardService,
                                    MarsContextProvider contextProvider,
                                    AiCardValidationService aiCardValidationService, DatasetCollectionService datasetCollectionService) {
        super(turnTypeService, stateFactory, stateContextProvider, turnProcessor);
        this.aiTurnService = aiTurnService;
        this.aiPaymentService = aiPaymentService;
        this.aiCardBuildParamsService = aiCardBuildParamsService;
        this.cardValidationService = cardValidationService;
        this.deepNetwork = deepNetwork;
        this.cardService = cardService;
        this.stateFactory = stateFactory;
        this.contextProvider = contextProvider;
        this.aiCardValidationService = aiCardValidationService;
        this.datasetCollectionService = datasetCollectionService;
    }

    public BuildProjectPrediction getBestProjectToBuild(MarsGame game, Player player, Phase phase, ProjectionStrategy projectionStrategy) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        projectPhasePick(game, player, phase, projectionStrategy);

        List<Card> availableCards = getAvailableCardsToBuild(game, player);

        if (availableCards.isEmpty()) {
            return BuildProjectPrediction.builder().canBuild(false).build();
        }

        Card bestCard = null;
        float bestChance = deepNetwork.testState(game, player);

        for (Card playableCard : availableCards) {
            MarsGame stateAfterPlayingTheCard = assumeProjectIsBuilt(game, player, playableCard);

            BuildProjectPrediction nextBestProjectToBuild = getBestProjectToBuild(stateAfterPlayingTheCard, stateAfterPlayingTheCard.getPlayerByUuid(player.getUuid()), phase, ProjectionStrategy.FROM_PHASE);

            float projectedChance = (nextBestProjectToBuild.isCanBuild() ? nextBestProjectToBuild.getExpectedValue() : deepNetwork.testState(stateAfterPlayingTheCard, stateAfterPlayingTheCard.getPlayerByUuid(player.getUuid())));

            if (projectedChance > bestChance) {
                bestChance = projectedChance;
                bestCard = playableCard;
            }
        }

        if (bestCard == null) {
            if (player.getBuilds().stream().map(BuildDto::getType).anyMatch(buildType -> buildType == BuildType.BLUE_RED_OR_CARD)) {
                BuildProjectPrediction stateAfterTakingCard = projectTakeCardSecondPhase(game, game.getPlayerByUuid(player.getUuid()));

                if (stateAfterTakingCard.isCanBuild() && stateAfterTakingCard.getExpectedValue() > bestChance) {
                    return stateAfterTakingCard;
                }
            } else if (player.getBuilds().stream().map(BuildDto::getType).anyMatch(buildType -> buildType == BuildType.BLUE_RED_OR_MC)) {
                player.setMc(player.getMc() + 6);

                return BuildProjectPrediction.builder().canBuild(true).card(null).expectedValue(deepNetwork.testState(game, game.getPlayerByUuid(player.getUuid()))).build();
            }
        }

        if (bestCard == null) {
            return BuildProjectPrediction.builder().canBuild(false).build();
        } else {
            return BuildProjectPrediction.builder().canBuild(true).expectedValue(bestChance).card(bestCard).build();
        }
    }


    private List<Card> getAvailableCardsToBuild(MarsGame game, Player player) {
        return player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> aiCardValidationService.isValid(game, player, card))
                .collect(Collectors.toList());
    }

    private BuildProjectPrediction projectTakeCardSecondPhase(MarsGame game, Player player) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        final MarsGameRow marsGameRow = datasetCollectionService.collectGameAndPlayers(
                game,
                player,
                players.get(0) == player
                        ? players.get(1)
                        : players.get(0)
        );

        MarsPlayerRow marsPlayerRow = marsGameRow.getPlayer();

        marsPlayerRow.setGreenCards(marsPlayerRow.getGreenCards() + GREEN_CARDS_RATIO);
        marsPlayerRow.setRedCards(marsPlayerRow.getRedCards() + RED_CARDS_RATIO);
        marsPlayerRow.setBlueCards(marsPlayerRow.getBlueCards() + BLUE_CARDS_RATIO);

        return BuildProjectPrediction.builder().canBuild(true).card(null).expectedValue(deepNetwork.testState(marsGameRow, player.isFirstBot() ? 1 : 2)).build();
    }

    public MarsGame projectBuildCardNoRequirements(MarsGame game, Player player, Card card) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        Map<Integer, List<Integer>> inputParameters = aiCardBuildParamsService.getInputParamsForBuild(game, player, card);
        List<Payment> payments = aiPaymentService.getCardPayments(game, player, card, inputParameters);
        if (card.getColor() == CardColor.GREEN) {
            aiTurnService.buildGreenProjectSyncNoRequirements(game, player, card.getId(), payments, inputParameters);
        } else {
            aiTurnService.buildBlueRedProjectSyncNoRequirements(game, player, card.getId(), payments, inputParameters);
        }

        return game;
    }

//    public GameWithState assumeProjectIsBuilt(MarsGame game, Player player, Card card, ProjectionStrategy projectionStrategy) {
//        projectPhasePick(game, player, card, projectionStrategy);
//
//        if (!aiCardValidationService.isValid(game, player, card)) {
//            throw new IllegalStateException("Card is not valid");
//        }
//
//        game = new MarsGame(game);
//        player = game.getPlayerByUuid(player.getUuid());
//
//        Map<Integer, List<Integer>> inputParams = aiCardBuildParamsService.getInputParamsForBuild(game, player, card);
//        List<Payment> payments = aiPaymentService.getCardPayments(game, player, card, inputParams);
//        if (card.getColor() == CardColor.GREEN) {
//            aiTurnService.buildGreenProjectSync(game, player, card.getId(), payments, inputParams);
//        } else {
//            aiTurnService.buildBlueRedProjectSync(game, player, card.getId(), payments, inputParams);
//        }
//
//        GameWithState gameWithState = new GameWithState();
//        gameWithState.setGame(game);
//
//        return gameWithState;
//    }

    //new method
    public MarsGame assumeProjectIsBuilt(MarsGame game, Player player, Card card) {
        if (!aiCardValidationService.isValid(game, player, card)) {
            throw new IllegalStateException("Card is not valid");
        }

        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        Map<Integer, List<Integer>> inputParams = aiCardBuildParamsService.getInputParamsForBuild(game, player, card);
        List<Payment> payments = aiPaymentService.getCardPayments(game, player, card, inputParams);
        if (card.getColor() == CardColor.GREEN) {
            aiTurnService.buildGreenProjectSync(game, player, card.getId(), payments, inputParams);
        } else {
            aiTurnService.buildBlueRedProjectSync(game, player, card.getId(), payments, inputParams);
        }

        return game;
    }

    //new method
    private void projectPhasePick(MarsGame game, Player player, Phase phase, ProjectionStrategy projectionStrategy) {
        if (projectionStrategy == ProjectionStrategy.FROM_PICK_PHASE) {
            final MarsContext context = contextProvider.provide(game, player);

            player.setPreviousChosenPhase(null);
            getAnotherPlayer(game, player).setPreviousChosenPhase(null);
            game.setStateType(StateType.PICK_PHASE, context);
            game.getPlayerUuidToPlayer().values().stream().filter(p -> !p.getUuid().equals(player.getUuid())).forEach(
                    anotherPlayer -> aiTurnService.choosePhaseTurn(anotherPlayer, phase == Phase.FIRST ? 2 : 1)
            );
            aiTurnService.choosePhaseTurn(player, phase == Phase.FIRST ? 1 : 2);
            while (processFinalTurns(game)) {
                stateFactory.getCurrentState(game).updateState();
            }
        }
    }

    private Player getAnotherPlayer(MarsGame game, Player player) {
        return game.getPlayerUuidToPlayer().values().stream().filter(p -> !p.getUuid().equals(player.getUuid()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Another player not found"));
    }
}
