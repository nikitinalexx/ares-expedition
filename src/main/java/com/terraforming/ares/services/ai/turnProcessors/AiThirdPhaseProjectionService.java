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
        int network = player.isFirstBot() ? 1 : 2;
        float stateBeforePhase = deepNetwork.testState(game, player, network);

        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        player.setChosenPhase(Constants.PERFORM_BLUE_ACTION_PHASE);
        player.setBlueActionExtraActivationsLeft(player.hasPhaseUpgrade(Constants.PHASE_3_UPGRADE_DOUBLE_REPEAT) ? 2 : 1);
        //project plants/heat  exchange
        //project opponent
        ProjectionWithGame projectionWithGame = projectThirdPhase(game, player, new MarsGameRowDifference(), null, network);
        if (projectionWithGame.isPickPhase()) {
            spendPlantsAndHeat(projectionWithGame.getGame(), projectionWithGame.getGame().getPlayerByUuid(player.getUuid()));

            List<Player> players = new ArrayList<>(projectionWithGame.getGame().getPlayerUuidToPlayer().values());
            Player anotherPlayer = players.get(0).getUuid().equals(player.getUuid()) ? players.get(1) : players.get(0);


            float futureState = deepNetwork.testState(
                    datasetCollectionService.collectGameAndPlayers(
                            projectionWithGame.getGame(),
                            projectionWithGame.getGame().getPlayerByUuid(player.getUuid()),
                            projectionWithGame.getGame().getPlayerByUuid(anotherPlayer.getUuid())
                    ).applyDifference(projectionWithGame.getDifference()),
                    network);

            if (Constants.LOG_NET_COMPARISON) {
                System.out.println("3 phase before  " + stateBeforePhase + "; after  " + futureState);
            }

            projectionWithGame.getProjection().setChance(futureState);

            game = new MarsGame(projectionWithGame.getGame());
            Player opponent = game.getPlayerByUuid(anotherPlayer.getUuid());
            opponent.setBlueActionExtraActivationsLeft(0);
            opponent.setActivatedBlueCards(Deck.builder().build());
            opponent.setChosenPhase(Constants.COLLECT_INCOME_PHASE);//set to anything but third phase

            ProjectionWithGame opponentProjection = projectThirdPhase(game, opponent, new MarsGameRowDifference(), projectionWithGame.getDifference(), network);
            if (!opponentProjection.isPickPhase()) {
                spendPlantsAndHeat(projectionWithGame.getGame(), projectionWithGame.getGame().getPlayerByUuid(anotherPlayer.getUuid()));

                futureState = deepNetwork.testState(
                        datasetCollectionService.collectGameAndPlayers(
                                projectionWithGame.getGame(),
                                projectionWithGame.getGame().getPlayerByUuid(player.getUuid()),
                                projectionWithGame.getGame().getPlayerByUuid(anotherPlayer.getUuid())
                        ).applyDifference(projectionWithGame.getDifference()), network);
                if (stateBeforePhase > futureState) {
                    if (Constants.LOG_NET_COMPARISON) {
                        System.out.println("3 phase skip because bad chance " + futureState);
                    }
                    return PhaseChoiceProjection.builder().pickPhase(false).phase(3).chance(futureState).build();
                }

                if (Constants.LOG_NET_COMPARISON) {
                    System.out.println("3 phase opponent will skip, main chance " + futureState);
                }

                projectionWithGame.getProjection().setChance(futureState);

                return projectionWithGame.getProjection();
            } else {
                spendPlantsAndHeat(opponentProjection.getGame(), opponentProjection.getGame().getPlayerByUuid(anotherPlayer.getUuid()));

                futureState = deepNetwork.testState(
                        datasetCollectionService.collectGameAndPlayers(
                                opponentProjection.getGame(),
                                opponentProjection.getGame().getPlayerByUuid(player.getUuid()),
                                        opponentProjection.getGame().getPlayerByUuid(anotherPlayer.getUuid())
                        )
                                .applyDifference(projectionWithGame.getDifference())
                                .applyOpponentDifference(opponentProjection.getDifference()), network);
                if (stateBeforePhase > futureState) {
                    if (Constants.LOG_NET_COMPARISON) {
                        System.out.println("3 phase skip because bad chance " + futureState);
                    }
                    return PhaseChoiceProjection.builder().pickPhase(false).phase(3).chance(futureState).build();
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

    public ProjectionWithGame projectThirdPhase(MarsGame game, Player player, MarsGameRowDifference initialDifference, MarsGameRowDifference opponentDifference, int network) {
        //TODO where to put Heat Exchange?
        //TODO project UNMI action
        //TODO project standard action

        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        MarsGameRow playerData = datasetCollectionService.collectGameAndPlayers(game, player, anotherPlayer);
//        if (playerData == null) {
//            return ProjectionWithGame.SKIP_PHASE;
//        }
        float bestState = deepNetwork.testState(playerData.applyDifference(initialDifference).applyOpponentDifference(opponentDifference), network);

        //project cards
        List<Card> activeCards = player.getPlayed().getCards().stream().map(cardService::getCard).filter(Card::isActiveCard).filter(card -> !player.getActivatedBlueCards().containsCard(card.getId()) || player.getBlueActionExtraActivationsLeft() != 0).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(activeCards)) {


            MarsGameRowDifference bestProjectionRow = null;
            MarsGame bestGameProjection = null;
            Card bestCard = null;

            for (Card activeCard : activeCards) {
                MarsGame gameCopy = new MarsGame(game);
                Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());

                MarsGameRowDifference difference = actionProjections.get(activeCard.getClass()).project(initialDifference, gameCopy, playerCopy, activeCard, network);
                difference.add(initialDifference);

                //TODO it still gives money even if the action was not played
                if (specialEffectsService.ownsSpecialEffect(playerCopy, SpecialEffect.ASSEMBLY_LINES)) {
                    playerCopy.setMc(playerCopy.getMc() + 1);
                }

                //TODO is it better to project 2 steps ahead? implement and compare 2 computers
                //TODO reuse anotherPlayer
                float futureState = deepNetwork.testState(datasetCollectionService.collectGameAndPlayers(gameCopy, playerCopy, anotherPlayer).applyDifference(difference).applyOpponentDifference(opponentDifference), network);

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
                ProjectionWithGame anotherProjection = projectThirdPhase(bestGameProjection, bestProjectionPlayer, bestProjectionRow, opponentDifference, network);
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

        float futureState = deepNetwork.testState(datasetCollectionService.collectGameAndPlayers(gameCopy, playerCopy, anotherPlayer).applyDifference(initialDifference).applyOpponentDifference(opponentDifference), network);

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
