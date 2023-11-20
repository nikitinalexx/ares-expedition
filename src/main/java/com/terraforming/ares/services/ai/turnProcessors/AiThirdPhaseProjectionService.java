package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.dataset.MarsGameRow;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.*;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.dto.PhaseChoiceProjection;
import com.terraforming.ares.services.ai.dto.ProjectionWithGame;
import com.terraforming.ares.services.ai.thirdPhaseCards.AiCardProjection;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiThirdPhaseProjectionService {
    private final Map<Class<?>, AiCardProjection<?>> actionProjections;
    private final CardService cardService;
    private final DeepNetwork deepNetwork;
    private final DatasetCollectionService datasetCollectionService;
    private final SpecialEffectsService specialEffectsService;
    private final PaymentValidationService paymentValidationService;
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    public AiThirdPhaseProjectionService(List<AiCardProjection<?>> actionProjections,
                                         CardService cardService,
                                         DeepNetwork deepNetwork,
                                         DatasetCollectionService datasetCollectionService,
                                         SpecialEffectsService specialEffectsService,
                                         PaymentValidationService paymentValidationService,
                                         TerraformingService terraformingService,
                                         MarsContextProvider marsContextProvider) {
        this.actionProjections = actionProjections.stream().collect(
                Collectors.toMap(
                        AiCardProjection::getType,
                        Function.identity()
                )
        );
        this.cardService = cardService;
        this.deepNetwork = deepNetwork;
        this.datasetCollectionService = datasetCollectionService;
        this.specialEffectsService = specialEffectsService;
        this.paymentValidationService = paymentValidationService;
        this.terraformingService = terraformingService;
        this.marsContextProvider = marsContextProvider;
    }

    public PhaseChoiceProjection projectThirdPhase(MarsGame game, Player player) {
//        if (player.isSecondBot() && player.getMc() > 50) {
//
//            float bestState = deepNetwork.testState(game, player);
//            int actionType = -1;
//
//
//            if (game.getPlanet().oceansLeft() > 0) {
//                float state = testAiService.projectPlayStandardAction(game, player.getUuid(), 1);
//                if (state > bestState) {
//                    bestState = state;
//                    actionType = 1;
//                }
//            }
//            if (game.getPlanet().oxygenLeft() > 0) {
//                float state = testAiService.projectPlayStandardAction(game, player.getUuid(), 2);
//                if (state > bestState) {
//                    bestState = state;
//                    actionType = 2;
//                }
//            }
//            if (game.getPlanet().temperatureLeft() > 0) {
//                float state = testAiService.projectPlayStandardAction(game, player.getUuid(), 3);
//                if (state > bestState) {
//                    bestState = state;
//                    actionType = 3;
//                }
//            }
//
//            if (actionType == 1) {
//                aiTurnService.standardProjectTurn(game, player, StandardProjectType.OCEAN);
//                return true;
//            } else if (actionType == 2) {
//                aiTurnService.standardProjectTurn(game, player, StandardProjectType.FOREST);
//                return true;
//            } else if (actionType == 3) {
//                aiTurnService.standardProjectTurn(game, player, StandardProjectType.TEMPERATURE);
//                return true;
//            }
//        }
        float stateBeforePhase = deepNetwork.testState(game, player);

        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());
        player.setBlueActionExtraActivationsLeft(1);
        //project plants/heat  exchange
        //project opponent
        ProjectionWithGame projectionWithGame = projectThirdPhase(game, player, new MarsGameRowDifference(), null);
        if (projectionWithGame.isPickPhase()) {
            spendPlantsAndHeat(projectionWithGame.getGame(), projectionWithGame.getGame().getPlayerByUuid(player.getUuid()));

            List<Player> players = new ArrayList<>(projectionWithGame.getGame().getPlayerUuidToPlayer().values());
            Player anotherPlayer = players.get(0).getUuid().equals(player.getUuid()) ? players.get(1) : players.get(0);


            float futureState = deepNetwork.testState(
                    datasetCollectionService.putPlayerRowIntoArray(
                            projectionWithGame.getGame(),
                            projectionWithGame.getGame().getPlayerByUuid(player.getUuid()),
                            projectionWithGame.getGame().getPlayerByUuid(anotherPlayer.getUuid())
                    ).applyDifference(projectionWithGame.getDifference()), player.isFirstBot() ? 1 : 2);

            if (Constants.LOG_NET_COMPARISON) {
                System.out.println("3 phase before  " + stateBeforePhase + "; after  " + futureState);
            }

            projectionWithGame.getProjection().setChance(futureState);

            game = new MarsGame(projectionWithGame.getGame());
            Player opponent = game.getPlayerByUuid(anotherPlayer.getUuid());
            opponent.setBlueActionExtraActivationsLeft(0);
            opponent.setActivatedBlueCards(Deck.builder().build());

            ProjectionWithGame opponentProjection = projectThirdPhase(game, opponent, new MarsGameRowDifference(), projectionWithGame.getDifference());
            if (!opponentProjection.isPickPhase()) {
                spendPlantsAndHeat(projectionWithGame.getGame(), projectionWithGame.getGame().getPlayerByUuid(anotherPlayer.getUuid()));

                futureState = deepNetwork.testState(
                        datasetCollectionService.putPlayerRowIntoArray(
                                projectionWithGame.getGame(),
                                projectionWithGame.getGame().getPlayerByUuid(player.getUuid()),
                                projectionWithGame.getGame().getPlayerByUuid(anotherPlayer.getUuid())
                        ).applyDifference(projectionWithGame.getDifference()), player.isFirstBot() ? 1 : 2);
                if (stateBeforePhase > futureState) {
                    if (Constants.LOG_NET_COMPARISON) {
                        System.out.println("3 phase skip because bad chance " + futureState);
                    }
                    return PhaseChoiceProjection.SKIP_PHASE;
                }

                if (Constants.LOG_NET_COMPARISON) {
                    System.out.println("3 phase opponent will skip, main chance " + futureState);
                }

                projectionWithGame.getProjection().setChance(futureState);

                return projectionWithGame.getProjection();
            } else {
                spendPlantsAndHeat(opponentProjection.getGame(), opponentProjection.getGame().getPlayerByUuid(anotherPlayer.getUuid()));

                futureState = deepNetwork.testState(
                        datasetCollectionService.putPlayerRowIntoArray(
                                opponentProjection.getGame(),
                                opponentProjection.getGame().getPlayerByUuid(player.getUuid()),
                                        opponentProjection.getGame().getPlayerByUuid(anotherPlayer.getUuid())
                        )
                                .applyDifference(projectionWithGame.getDifference())
                                .applyOpponentDifference(opponentProjection.getDifference()), player.isFirstBot() ? 1 : 2);
                if (stateBeforePhase > futureState) {
                    if (Constants.LOG_NET_COMPARISON) {
                        System.out.println("3 phase skip because bad chance " + futureState);
                    }
                    return PhaseChoiceProjection.SKIP_PHASE;
                }

                if (Constants.LOG_NET_COMPARISON) {
                    System.out.println("3 phase opponent will play, main chance " + futureState);
                }

                projectionWithGame.getProjection().setChance(futureState);

                return projectionWithGame.getProjection();
            }
        }

        //todo what if spending heat,plants + standard project is a better turn than skip phase?
        return PhaseChoiceProjection.SKIP_PHASE;
    }

    public ProjectionWithGame projectThirdPhase(MarsGame game, Player player, MarsGameRowDifference initialDifference, MarsGameRowDifference opponentDifference) {
        //TODO where to put Heat Exchange?
        //TODO project UNMI action
        //TODO project standard action

        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        MarsGameRow playerData = datasetCollectionService.putPlayerRowIntoArray(game, player, anotherPlayer);
        if (playerData == null) {
            return ProjectionWithGame.SKIP_PHASE;
        }
        float bestState = deepNetwork.testState(playerData.applyDifference(initialDifference).applyOpponentDifference(opponentDifference), player.isFirstBot() ? 1 : 2);

        //project cards
        List<Card> activeCards = player.getPlayed().getCards().stream().map(cardService::getCard).filter(Card::isActiveCard).filter(card -> !player.getActivatedBlueCards().containsCard(card.getId()) || player.getBlueActionExtraActivationsLeft() != 0).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(activeCards)) {


            MarsGameRowDifference bestProjectionRow = null;
            MarsGame bestGameProjection = null;
            Card bestCard = null;

            for (Card activeCard : activeCards) {
                MarsGame gameCopy = new MarsGame(game);
                Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());

                MarsGameRowDifference difference = actionProjections.get(activeCard.getClass()).project(initialDifference, gameCopy, playerCopy, activeCard);
                difference.add(initialDifference);

                if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.ASSEMBLY_LINES)) {
                    playerCopy.setMc(playerCopy.getMc() + 1);
                }

                //TODO is it better to project 2 steps ahead? implement and compare 2 computers
                //TODO reuse anotherPlayer
                float futureState = deepNetwork.testState(datasetCollectionService.putPlayerRowIntoArray(gameCopy, playerCopy, anotherPlayer).applyDifference(difference).applyOpponentDifference(opponentDifference), player.isFirstBot() ? 1 : 2);

                if (futureState > bestState) {
                    bestState = futureState;
                    bestProjectionRow = difference;
                    bestGameProjection = gameCopy;
                    bestCard = activeCard;
                }
            }

            if (bestProjectionRow != null) {
                Player bestProjectionPlayer = bestGameProjection.getPlayerByUuid(player.getUuid());
                Deck activatedBlueCards = bestProjectionPlayer.getActivatedBlueCards();
                if (activatedBlueCards.containsCard(bestCard.getId())) {
                    bestProjectionPlayer.setBlueActionExtraActivationsLeft(bestProjectionPlayer.getBlueActionExtraActivationsLeft() - 1);
                } else {
                    activatedBlueCards.addCard(bestCard.getId());
                }
                ProjectionWithGame anotherProjection = projectThirdPhase(bestGameProjection, bestProjectionPlayer, bestProjectionRow, opponentDifference);
                if (anotherProjection.isPickPhase()) {
                    return anotherProjection;
                }
                return ProjectionWithGame.builder()
                        .projection(PhaseChoiceProjection.builder().phase(3).pickPhase(true).chance(bestState).build())
                        .game(bestGameProjection)
                        .difference(bestProjectionRow)
                        .build();
            }
        }

        MarsGame gameCopy = new MarsGame(game);
        Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());

        spendPlantsAndHeat(gameCopy, playerCopy);

        float futureState = deepNetwork.testState(datasetCollectionService.putPlayerRowIntoArray(gameCopy, playerCopy, anotherPlayer).applyDifference(initialDifference).applyOpponentDifference(opponentDifference), player.isFirstBot() ? 1 : 2);

        if (futureState > bestState) {
            return ProjectionWithGame.builder()
                    .projection(PhaseChoiceProjection.builder().phase(3).pickPhase(true).chance(bestState).build())
                    .game(gameCopy)
                    .difference(initialDifference)
                    .build();
        }

        return ProjectionWithGame.SKIP_PHASE;
    }

    private void spendPlantsAndHeat(MarsGame game, Player player) {
        final MarsContext context = marsContextProvider.provide(game, player);
        final int plantsPrice = paymentValidationService.forestPriceInPlants(player);

        while (player.getPlants() >= plantsPrice) {
            terraformingService.buildForest(context);
            player.setPlants(player.getPlants() - plantsPrice);
        }

        if (terraformingService.canIncreaseTemperature(game)) {
            while (player.getHeat() >= Constants.TEMPERATURE_HEAT_COST) {
                terraformingService.increaseTemperature(context);
                player.setHeat(player.getHeat() - 8);
            }
        }
    }

}
