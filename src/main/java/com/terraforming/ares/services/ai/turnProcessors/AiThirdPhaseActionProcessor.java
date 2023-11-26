package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.ai.AiExperimentalTurn;
import com.terraforming.ares.model.ai.AiTurnChoice;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.ai.AiCardValidationService;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.ProjectionStrategy;
import com.terraforming.ares.services.ai.TestAiService;
import com.terraforming.ares.services.ai.dto.ActionInputParamsResponse;
import com.terraforming.ares.services.ai.dto.BuildProjectPrediction;
import com.terraforming.ares.services.ai.helpers.AiCardActionHelper;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsService;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
public class AiThirdPhaseActionProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final AiPaymentService aiPaymentHelper;
    private final AiCardActionHelper aiCardActionHelper;
    private final StandardProjectService standardProjectService;
    private final AiCardBuildParamsService aiCardBuildParamsService;
    private final TestAiService testAiService;
    private final AiCardValidationService aiCardValidationService;
    private final AiBuildProjectService aiBuildProjectService;
    private final DeepNetwork deepNetwork;
    private final CardValidationService cardValidationService;

    public AiThirdPhaseActionProcessor(AiTurnService aiTurnService,
                                       CardService cardService,
                                       AiPaymentService aiPaymentHelper,
                                       AiCardActionHelper aiCardActionHelper,
                                       StandardProjectService standardProjectService,
                                       AiCardBuildParamsService aiCardBuildParamsService,
                                       TestAiService testAiService,
                                       AiCardValidationService aiCardValidationService, AiBuildProjectService aiBuildProjectService, DeepNetwork deepNetwork, CardValidationService cardValidationService) {
        this.aiTurnService = aiTurnService;
        this.cardService = cardService;
        this.aiPaymentHelper = aiPaymentHelper;
        this.aiCardActionHelper = aiCardActionHelper;
        this.standardProjectService = standardProjectService;
        this.aiCardBuildParamsService = aiCardBuildParamsService;
        this.testAiService = testAiService;
        this.aiCardValidationService = aiCardValidationService;
        this.aiBuildProjectService = aiBuildProjectService;
        this.deepNetwork = deepNetwork;
        this.cardValidationService = cardValidationService;
    }

    public boolean processTurn(List<TurnType> possibleTurns, MarsGame game, Player player) {
        boolean madeATurn = makeATurn(possibleTurns, game, player);
        if (madeATurn) {
            return true;
        } else if (doStandardActionsIfAvailable(possibleTurns, game, player)) {
            return true;
        }

        if (game.gameEndCondition()) {
            if (player.getHand().size() != 0) {
                aiTurnService.sellCards(player, game, player.getHand().getCards());
                return true;
            } else {
                switch (player.getDifficulty().THIRD_PHASE_ACTION) {
                    case RANDOM:
                    case SMART: {
                        if (!game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
                            String validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.FOREST);
                            if (validationResult == null) {
                                aiTurnService.standardProjectTurn(game, player, StandardProjectType.FOREST);
                                return true;
                            }
                        }
                        String validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.OCEAN);
                        if (validationResult == null) {
                            aiTurnService.standardProjectTurn(game, player, StandardProjectType.OCEAN);
                            return true;
                        }
                        validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.TEMPERATURE);
                        if (validationResult == null) {
                            aiTurnService.standardProjectTurn(game, player, StandardProjectType.TEMPERATURE);
                            return true;
                        }
                        validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.FOREST);
                        if (validationResult == null) {
                            aiTurnService.standardProjectTurn(game, player, StandardProjectType.FOREST);
                            return true;
                        }
                    }
                    break;
                    case NETWORK: {
                        StandardProjectType type = null;
                        float bestState = -1;

                        for (StandardProjectType standardProjectType : List.of(StandardProjectType.FOREST, StandardProjectType.OCEAN, StandardProjectType.TEMPERATURE)) {
                            String validationResult = standardProjectService.validateStandardProject(game, player, standardProjectType);
                            if (validationResult == null) {
                                float projectedChance = testAiService.projectPlayStandardAction(game, player.getUuid(), standardProjectType);
                                if (projectedChance > bestState) {
                                    type = standardProjectType;
                                    bestState = projectedChance;
                                }
                            }
                        }

                        if (type != null) {
                            aiTurnService.standardProjectTurn(game, player, type);
                            return true;
                        }
                    }
                    break;
                }
            }

            aiTurnService.confirmGameEnd(game, player);
            return true;
        }

        aiTurnService.skipTurn(player);
        return true;
    }


    private boolean makeATurn(List<TurnType> possibleTurns, MarsGame game, Player player) {
        //TODO what if you have blue cards that spend heat
        if (player.getSelectedCorporationCard() == 10000 || player.getSelectedCorporationCard() == 10100) {
            if (game.getPlanetAtTheStartOfThePhase().isTemperatureMax() && player.getHeat() > 0) {
                player.setMc(player.getMc() + player.getHeat());
                player.setHeat(0);
            }
        }
        Deck activatedBlueCards = player.getActivatedBlueCards();

        List<Card> notUsedBlueCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(c -> !activatedBlueCards.containsCard(c.getId()))
                .collect(Collectors.toList());

        boolean actionPerformed = performCardAction(game, player, notUsedBlueCards);

        if (actionPerformed) {
            return true;
        }

        if (player.getChosenPhase() == 3 && player.getBlueActionExtraActivationsLeft() > 0) {
            List<Card> actionBlueCards = player.getPlayed().getCards().stream()
                    .map(cardService::getCard)
                    .filter(Card::isActiveCard)
                    .collect(Collectors.toList());

            actionPerformed = performCardAction(game, player, actionBlueCards);
        }

        if (actionPerformed) {
            return true;
        }

//        Set<CardAction> playedActions = player.getPlayed().getCards().stream().map(cardService::getCard)
//                .map(Card::getCardMetadata)
//                .filter(Objects::nonNull)
//                .map(CardMetadata::getCardAction)
//                .filter(Objects::nonNull).collect(Collectors.toSet());
//
//        //TODO looks very artificial
//        if (!game.getPlanetAtTheStartOfThePhase().isOceansMax()) {
//            if ((playedActions.contains(CardAction.ARCTIC_ALGAE)
//                    || playedActions.contains(CardAction.FISH))
//                    && player.getMc() > 30) {
//                aiTurnService.standardProjectTurn(game, player, StandardProjectType.OCEAN);
//                return true;
//            }
//        }
//        if (!game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
//            if ((playedActions.contains(CardAction.LIVESTOCK))
//                    && player.getMc() > 30) {
//                aiTurnService.standardProjectTurn(game, player, StandardProjectType.TEMPERATURE);
//                return true;
//            }
//            if ((playedActions.contains(CardAction.PHYSICS_COMPLEX))
//                    && player.getMc() > 70) {
//                aiTurnService.standardProjectTurn(game, player, StandardProjectType.TEMPERATURE);
//                return true;
//            }
//        }
//
        if (!game.gameEndCondition() && canFinishGame(game, player) && (player.getDifficulty().THIRD_PHASE_ACTION != AiTurnChoice.NETWORK || deepNetwork.testState(game, player) >= 0.6)) {
            if (player.getHand().size() != 0) {
                aiTurnService.sellCards(player, game, player.getHand().getCards());
                return true;
            }
            if (game.getPlanet().temperatureLeft() > 0) {
                aiTurnService.standardProjectTurn(game, player, StandardProjectType.TEMPERATURE);
                return true;
            }
            if (game.getPlanet().oxygenLeft() > 0) {
                aiTurnService.standardProjectTurn(game, player, StandardProjectType.FOREST);
                return true;
            }
            if (game.getPlanet().oceansLeft() > 0) {
                aiTurnService.standardProjectTurn(game, player, StandardProjectType.OCEAN);
                return true;
            }
        }

        boolean buildProjects = buildProjectsIfPossible(game, player, possibleTurns);

        if (buildProjects) {
            return true;
        }

        if (player.getDifficulty().EXPERIMENTAL_TURN == AiExperimentalTurn.EXPERIMENT) {
            float stateBeforeStandardProject = deepNetwork.testState(game, player);
            StandardProjectType bestStandardProject = null;

            for (StandardProjectType standardProjectType : List.of(StandardProjectType.FOREST, StandardProjectType.OCEAN, StandardProjectType.TEMPERATURE)) {
                float nextState = testAiService.projectPlayStandardAction(game, player.getUuid(), StandardProjectType.FOREST);
                if (nextState > stateBeforeStandardProject) {
                    stateBeforeStandardProject = nextState;
                    bestStandardProject = standardProjectType;
                }
            }
            if (bestStandardProject != null) {
                aiTurnService.standardProjectTurn(game, player, bestStandardProject);
            }
        }

        return false;
    }

    private boolean buildProjectsIfPossible(MarsGame game, Player player, List<TurnType> possibleTurns) {
        if (!player.getBuilds().isEmpty()) {

            Card cardToBuild = null;

            switch (player.getDifficulty().THIRD_PHASE_ACTION) {
                case NETWORK:
                    BuildProjectPrediction bestProjectToBuild = aiBuildProjectService.getBestProjectToBuild(
                            game, player, null, ProjectionStrategy.FROM_PHASE
                    );
                    if (bestProjectToBuild.isCanBuild()) {
                        cardToBuild = bestProjectToBuild.getCard();
                        break;
                    } else {
                        //go to RANDOM switch case
                    }
                case RANDOM:
                case SMART:
                    List<Card> availableCards = player.getHand()
                            .getCards()
                            .stream()
                            .map(cardService::getCard)
                            .filter(card -> (possibleTurns.contains(TurnType.BUILD_BLUE_RED_PROJECT) && card.getColor() != CardColor.GREEN)
                                    || (possibleTurns.contains(TurnType.BUILD_GREEN_PROJECT) && card.getColor() == CardColor.GREEN))
                            .filter(card -> aiCardValidationService.isValid(game, player, card))
                            .collect(Collectors.toList());

                    if (!availableCards.isEmpty()) {
                        cardToBuild = availableCards.stream().max(Comparator.comparingInt(Card::getPrice)).orElseThrow();
                    }
            }

            if (cardToBuild != null) {
                Map<Integer, List<Integer>> inputParams = aiCardBuildParamsService.getInputParamsForBuild(game, player, cardToBuild);
                aiTurnService.buildProject(
                        game, player, cardToBuild.getId(), aiPaymentHelper.getCardPayments(game, player, cardToBuild, inputParams),
                        inputParams
                );
                return true;
            }
        }

        return false;
    }

    private boolean doStandardActionsIfAvailable(List<TurnType> possibleTurns, MarsGame game, Player player) {
        if (possibleTurns.contains(TurnType.INCREASE_TEMPERATURE)) {
            aiTurnService.increaseTemperature(game, player);
            return true;
        }
        if (possibleTurns.contains(TurnType.PLANT_FOREST)) {
            aiTurnService.plantForest(game, player);
            return true;
        }
        return false;
    }

    private boolean canFinishGame(MarsGame game, Player player) {
        int mc = player.getMc();
        mc += player.getHand().size() * 3;

        mc -= game.getPlanet().oceansLeft() * standardProjectService.getProjectPrice(player, StandardProjectType.OCEAN);

        if (mc < 0) {
            return false;
        }

        mc -= game.getPlanet().temperatureLeft() * standardProjectService.getProjectPrice(player, StandardProjectType.TEMPERATURE);

        if (mc < 0) {
            return false;
        }

        mc -= game.getPlanet().oxygenLeft() * standardProjectService.getProjectPrice(player, StandardProjectType.FOREST);

        return mc >= 0;
    }

    private final Set<Class<?>> ACTIONS_THAT_REQUIRE_NETWORK_VALIDATION = Set.of(
            AquiferPumping.class,
            DevelopedInfrastructure.class,
            SolarPunk.class,
            VolcanicPools.class,
            WaterImportFromEuropa.class,
            ProgressivePolicies.class,
            ExperimentalTechnology.class,
            CommunityAfforestation.class,
            GasCooledReactors.class
    );

    private boolean performCardAction(MarsGame game, Player player, List<Card> cards) {
        if (cards.isEmpty()) {
            return false;
        }
        cards = new ArrayList<>(cards);

        List<Card> cardsThatDontRequireNetworkValidation = List.of();
        List<Card> cardsThatRequireNetworkValidation = List.of();

        switch (player.getDifficulty().THIRD_PHASE_ACTION) {
            case RANDOM:
            case SMART:
                cardsThatDontRequireNetworkValidation = cards;
                break;
            case NETWORK:
                cardsThatDontRequireNetworkValidation = cards.stream()
                        .filter(card -> !ACTIONS_THAT_REQUIRE_NETWORK_VALIDATION.contains(card.getClass()))
                        .collect(Collectors.toList());
                cardsThatRequireNetworkValidation = cards.stream()
                        .filter(card -> ACTIONS_THAT_REQUIRE_NETWORK_VALIDATION.contains(card.getClass()))
                        .collect(Collectors.toList());
                break;
        }

        while (!cardsThatDontRequireNetworkValidation.isEmpty()) {
            int selectedIndex = random.nextInt(cardsThatDontRequireNetworkValidation.size());
            Card selectedCard = cardsThatDontRequireNetworkValidation.get(selectedIndex);

            if (aiCardActionHelper.isUsablePlayAction(game, player, selectedCard)) {
                ActionInputParamsResponse inputParams = aiCardActionHelper.getActionInputParamsForSmart(game, player, selectedCard);


                if (inputParams.isMakeAction()) {
                    aiTurnService.performBlueAction(
                            game,
                            player,
                            selectedCard.getId(),
                            inputParams.getInputParams()
                    );
                    return true;
                }
            }

            cardsThatDontRequireNetworkValidation.remove(selectedIndex);
        }

        if (!cardsThatRequireNetworkValidation.isEmpty()) {
            Integer bestCard = null;
            float bestChance = deepNetwork.testState(game, player);

            for (Card card : cardsThatRequireNetworkValidation) {
                ActionInputParamsResponse paramsResponse = aiCardActionHelper.getActionInputParamsForSmart(game, player, card);

                if (!paramsResponse.isMakeAction()) {
                    continue;
                }
                String validationResult = cardValidationService.validateBlueAction(player, game, card.getId(), paramsResponse.getInputParams());

                if (validationResult == null) {
                    MarsGame gameCopy = new MarsGame(game);
                    Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());
                    aiTurnService.performBlueAction(
                            gameCopy,
                            playerCopy,
                            card.getId(),
                            paramsResponse.getInputParams()
                    );
                    int handDifference = playerCopy.getHand().size() - player.getHand().size();
                    if (handDifference > 0) {
                        playerCopy.setHand(player.getHand());
                        playerCopy.setMc(playerCopy.getMc() + 3 * handDifference);
                    }
                    float newChance = deepNetwork.testState(gameCopy, playerCopy);
                    if (newChance > bestChance) {
                        bestChance = newChance;
                        bestCard = card.getId();
                    }
                }
            }
            if (bestCard != null) {
                ActionInputParamsResponse paramsResponse = aiCardActionHelper.getActionInputParamsForSmart(game, player, cardService.getCard(bestCard));
                aiTurnService.performBlueAction(
                        game,
                        player,
                        bestCard,
                        paramsResponse.getInputParams()
                );
                return true;
            }
        }

        return false;

//        switch (player.getDifficulty().THIRD_PHASE_ACTION) {
//            case RANDOM:
//            case SMART:
//                while (!cards.isEmpty()) {
//                    int selectedIndex = random.nextInt(cards.size());
//                    Card selectedCard = cards.get(selectedIndex);
//
//                    if (aiCardActionHelper.isUsablePlayAction(game, player, selectedCard)) {
//                        ActionInputParamsResponse inputParams = aiCardActionHelper.getActionInputParamsForSmart(game, player, selectedCard);
//
//
//                        if (inputParams.isMakeAction()) {
//                            aiTurnService.performBlueAction(
//                                    game,
//                                    player,
//                                    selectedCard.getId(),
//                                    inputParams.getInputParams()
//                            );
//                            return true;
//                        }
//                    }
//
//                    cards.remove(selectedIndex);
//                }
//                return false;
//            case NETWORK:
//                Integer bestCard = null;
//                float bestChance = deepNetwork.testState(game, player);
//
//                for (Card card : cards) {
//                    ActionInputParamsResponse paramsResponse = aiCardActionHelper.getActionInputParamsForSmart(game, player, card);
//
//                    if (!paramsResponse.isMakeAction()) {
//                        continue;
//                    }
//                    String validationResult = cardValidationService.validateBlueAction(player, game, card.getId(), paramsResponse.getInputParams());
//
//                    if (validationResult == null) {
//                        MarsGame gameCopy = new MarsGame(game);
//                        Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());
//                        aiTurnService.performBlueAction(
//                                gameCopy,
//                                playerCopy,
//                                card.getId(),
//                                paramsResponse.getInputParams()
//                        );
//                        float newChance = deepNetwork.testState(gameCopy, playerCopy);
//                        if (newChance > bestChance) {
//                            bestChance = newChance;
//                            bestCard = card.getId();
//                        }
//                    }
//                }
//                if (bestCard != null) {
//                    ActionInputParamsResponse paramsResponse = aiCardActionHelper.getActionInputParamsForSmart(game, player, cardService.getCard(bestCard));
//                    aiTurnService.performBlueAction(
//                            game,
//                            player,
//                            bestCard,
//                            paramsResponse.getInputParams()
//                    );
//                    return true;
//                }
//        }
    }

}