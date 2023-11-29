package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.dataset.MarsGameRow;
import com.terraforming.ares.dataset.MarsPlayerRow;
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
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.terraforming.ares.model.Constants.COLLECT_INCOME_PHASE;
import static com.terraforming.ares.model.Constants.LOG_NET_COMPARISON;

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

        Set<Integer> pickedPhases = game.getPlayerUuidToPlayer()
                .values()
                .stream()
                .map(Player::getChosenPhase)
                .collect(Collectors.toSet());

        if (game.isDummyHandMode() && !CollectionUtils.isEmpty(game.getUsedDummyHand())) {
            pickedPhases.add(game.getCurrentDummyHand());
        }

        //if both 1 and 2 phase are picked and now is 1, we check if something better can be build in phase 2
        BuildProjectPrediction bestFutureProject =
                (pickedPhases.contains(2) && game.getCurrentPhase() == 1)
                        ? getBestProjectToBuild(game, player, player.getChosenPhase() == 2 ? Phase.SECOND : Phase.SECOND_BY_ANOTHER, ProjectionStrategy.FROM_PICK_PHASE)
                        : BuildProjectPrediction.builder().build();

        //if 1 is picked, 2 is not picked and something good is in phase 2
        //if 2 is picked, 1 is not picked and something good is in phase 1
        BuildProjectPrediction anotherBestProject = BuildProjectPrediction.builder().build();
        if ((game.getCurrentPhase() == 1 || game.getCurrentPhase() == 2) && (!pickedPhases.contains(1) || !pickedPhases.contains(2)) && player.getChosenPhase() != game.getCurrentPhase()) {
            anotherBestProject = getBestProjectToBuild(game, player, game.getCurrentPhase() == 2 ? Phase.FIRST : Phase.SECOND, ProjectionStrategy.FROM_PICK_PHASE);
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

        if (bestCard != null && bestFutureProject.isCanBuild()) {
            MarsGame stateAfterPlayingBestCard = assumeProjectIsBuilt(game, player, bestCard);

            BuildProjectPrediction bestBuildFirstPlusSecondPhase = getBestProjectToBuild(stateAfterPlayingBestCard, stateAfterPlayingBestCard.getPlayerByUuid(player.getUuid()), (player.getChosenPhase() == 2 ? Phase.SECOND : Phase.SECOND_BY_ANOTHER), ProjectionStrategy.FROM_PICK_PHASE);

            if (!bestBuildFirstPlusSecondPhase.isCanBuild() && bestFutureProject.getExpectedValue() > bestChance) {
                if (LOG_NET_COMPARISON) {
                    System.out.println("Skipping " + bestCard + " to be able to build " + bestFutureProject.getCard());
                }
                bestCard = null;
            }
        }

        if (bestCard != null && anotherBestProject.isCanBuild()) {
            MarsGame stateAfterPlayingBestCardThisPhase = assumeProjectIsBuilt(game, player, bestCard);

            BuildProjectPrediction stateAfterPlayingBothPhases = getBestProjectToBuild(stateAfterPlayingBestCardThisPhase, stateAfterPlayingBestCardThisPhase.getPlayerByUuid(player.getUuid()), (game.getCurrentPhase() == 2 ? Phase.FIRST : Phase.SECOND), ProjectionStrategy.FROM_PICK_PHASE);

            if (!stateAfterPlayingBothPhases.isCanBuild() && anotherBestProject.getExpectedValue() > bestChance) {
                if (LOG_NET_COMPARISON) {
                    System.out.println("Didn't choose " + game.getCurrentPhase() + ". Skipping " + bestCard.getClass().getSimpleName() + " to be able to build " + anotherBestProject.getCard());
                }
                bestCard = null;
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


    public List<Card> getAvailableCardsToBuild(MarsGame game, Player player) {
        boolean canBuildGreen = player.canBuildGreen();
        boolean canBuildBlueRed = player.canBuildBlueRed();

        return player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> canBuildGreen && card.getColor() == CardColor.GREEN ||
                        canBuildBlueRed && (card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED)
                )
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

        marsPlayerRow.setCards(marsPlayerRow.getCards() + 1);

        return BuildProjectPrediction.builder().canBuild(true).card(null).expectedValue(deepNetwork.testState(marsGameRow, player.isFirstBot() ? 1 : 2)).build();
    }

    public MarsGame projectBuildCardNoRequirements(MarsGame game, Player player, Card card) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        Map<Integer, List<Integer>> inputParameters = aiCardBuildParamsService.getInputParamsForBuild(game, player, card, true);
        List<Payment> payments = aiPaymentService.getCardPayments(game, player, card, inputParameters);
        if (card.getColor() == CardColor.GREEN) {
            aiTurnService.buildGreenProjectSyncNoRequirements(game, player, card.getId(), payments, inputParameters);
        } else {
            aiTurnService.buildBlueRedProjectSyncNoRequirements(game, player, card.getId(), payments, inputParameters);
        }

        return game;
    }

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

    public MarsGame assumeProjectIsBuiltFromPickPhase(MarsGame game, Player player, Card card) {
        projectPhasePick(game, player, card.getColor() == CardColor.GREEN ? Phase.FIRST : Phase.SECOND, ProjectionStrategy.FROM_PICK_PHASE);

        if (!aiCardValidationService.isValid(game, player, card)) {
            System.out.println(card);
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
            game.setDummyHandMode(false);

            final MarsContext context = contextProvider.provide(game, player);

            player.setPreviousChosenPhase(null);
            getAnotherPlayer(game, player).setPreviousChosenPhase(null);
            game.setStateType(StateType.PICK_PHASE, context);
            game.getPlayerUuidToPlayer().values().stream().filter(p -> !p.getUuid().equals(player.getUuid())).forEach(
                    anotherPlayer -> aiTurnService.choosePhaseTurn(anotherPlayer, phase == Phase.SECOND_BY_ANOTHER ? 2 : COLLECT_INCOME_PHASE)
            );
            aiTurnService.choosePhaseTurn(player, phase == Phase.SECOND_BY_ANOTHER ? COLLECT_INCOME_PHASE : (phase == Phase.FIRST ? 1 : 2));
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
