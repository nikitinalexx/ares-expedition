package com.terraforming.ares.services.ai;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.dataset.MarsGameRow;
import com.terraforming.ares.dataset.MarsPlayerRow;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.ai.dto.BuildProjectPrediction;
import com.terraforming.ares.services.ai.turnProcessors.AiBuildProjectService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TestAiService {
    private final AiBuildProjectService aiBuildProjectService;
    private final DeepNetwork deepNetwork;
    private final StandardProjectService standardProjectService;
    private final AiTurnService aiTurnService;
    private final DatasetCollectionService datasetCollectionService;
    private final DraftCardsService draftCardsService;
    private final AiCollectIncomePhaseService aiCollectIncomePhaseService;
    private final CardService cardService;
    private final MarsContextProvider marsContextProvider;
    private final AiDiscoveryDecisionService aiDiscoveryDecisionService;
    private final AiPickCardProjectionService aiPickCardProjectionService;


    public BuildProjectPrediction getBestCardToBuild(MarsGame game, Player player, Set<CardColor> colors) {
        BuildProjectPrediction bestProject = BuildProjectPrediction.builder().build();

        if (colors.contains(CardColor.GREEN)) {
            BuildProjectPrediction greenProject = aiBuildProjectService.getBestProjectToBuild(game, player, Phase.FIRST, ProjectionStrategy.FROM_PICK_PHASE);
            if (Constants.LOG_NET_COMPARISON && greenProject.isCanBuild()) {
                System.out.println("Best GREEN " + (greenProject.getCard().getClass().getSimpleName()) + " " + greenProject.getExpectedValue());
            }
            bestProject = greenProject;
        }

        if (colors.contains(CardColor.RED) || colors.contains(CardColor.BLUE)) {
            BuildProjectPrediction redProject = aiBuildProjectService.getBestProjectToBuild(game, player, Phase.SECOND, ProjectionStrategy.FROM_PICK_PHASE);
            if (Constants.LOG_NET_COMPARISON && redProject.isCanBuild()) {
                if (redProject.getCard() != null) {
                    System.out.println("Best RED " + (redProject.getCard().getClass().getSimpleName()) + " " + redProject.getExpectedValue());
                } else {
                    System.out.println("Best RED take card " + redProject.getExpectedValue());
                }
            }

            if (!bestProject.isCanBuild() || redProject.isCanBuild() && redProject.getExpectedValue() >= bestProject.getExpectedValue()) {
                bestProject = redProject;
            }
        }

        if (bestProject.isCanBuild() && Constants.LOG_NET_COMPARISON) {
            System.out.println();
        }

        return bestProject;
    }

    public float projectPlayStandardAction(MarsGame game, String playerUuid, StandardProjectType type) {

        game = new MarsGame(game);

        Player player = game.getPlayerByUuid(playerUuid);

        int mc = player.getMc();

        if (type == StandardProjectType.OCEAN && mc >= standardProjectService.getProjectPrice(player, type)) {
            aiTurnService.standardProjectTurn(game, player, StandardProjectType.OCEAN);
            return deepNetwork.testState(game, player);
        }

        if (type == StandardProjectType.FOREST && mc >= standardProjectService.getProjectPrice(player, type)) {
            aiTurnService.standardProjectTurn(game, player, StandardProjectType.FOREST);
            return deepNetwork.testState(game, player);
        }

        if (type == StandardProjectType.TEMPERATURE && mc >= standardProjectService.getProjectPrice(player, type)) {
            aiTurnService.standardProjectTurn(game, player, StandardProjectType.TEMPERATURE);
            return deepNetwork.testState(game, player);
        }

        return -1;
    }

    public float projectPlayPhase4(MarsGame game, Player player) {

        game = new MarsGame(game);

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        player = game.getPlayerByUuid(player.getUuid());
        Player anotherPlayer = players.get(0) == player
                ? players.get(1)
                : players.get(0);

        BuildProjectPrediction bestPredictionBeforeIncome = getBestCardToBuild(game, player, Set.of(CardColor.GREEN, CardColor.RED, CardColor.BLUE));

        addMainIncome(player);
        addMainIncome(anotherPlayer);

        if (player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_EXTRA_MC)) {
            player.setMc(player.getMc() + 7);
        } else if (player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_DOUBLE_PRODUCE)) {
            player.setMc(player.getMc() + 1);
        }

        if (player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_DOUBLE_PRODUCE)) {
            Integer doubleIncomeCardId = aiCollectIncomePhaseService.getDoubleIncomeCard(game, player);
            if (doubleIncomeCardId != null) {
                Card doubleIncomeCard = cardService.getCard(doubleIncomeCardId);
                doubleIncomeCard.payAgain(game, cardService, player);
            }
        }

        {
            float bestStateBeforeBuild = deepNetwork.testState(game, player);

            BuildProjectPrediction prediction = getBestCardToBuild(game, player, Set.of(CardColor.GREEN, CardColor.RED, CardColor.BLUE));


            if (prediction.isCanBuild() && prediction.getCard() != null && (!bestPredictionBeforeIncome.isCanBuild() || bestPredictionBeforeIncome.getCard() != prediction.getCard())) {
                float relativeRatio = (prediction.getExpectedValue() - (bestPredictionBeforeIncome.isCanBuild() ? bestPredictionBeforeIncome.getExpectedValue() : bestStateBeforeBuild)) / (1 - bestStateBeforeBuild);
                if (relativeRatio >= 0.25f && prediction.getExpectedValue() != 1.0 && (!bestPredictionBeforeIncome.isCanBuild() || prediction.getExpectedValue() - bestPredictionBeforeIncome.getExpectedValue() >= 0.1f)) {
                    game = aiBuildProjectService.assumeProjectIsBuiltFromPickPhase(game, player, prediction.getCard());
                    player = game.getPlayerByUuid(player.getUuid());
                    anotherPlayer = game.getPlayerByUuid(anotherPlayer.getUuid());

                    if (Constants.LOG_NET_COMPARISON) {
                        Card bestCardBeforeIncome = bestPredictionBeforeIncome.isCanBuild() ? bestPredictionBeforeIncome.getCard() : null;
                        Card bestCardAfterIncome = prediction.isCanBuild() ? prediction.getCard() : null;
                        if (bestCardAfterIncome != bestCardBeforeIncome) {
                            System.out.println(
                                    "CardBeforeIncome " + (bestCardBeforeIncome != null ? bestCardBeforeIncome.getClass().getSimpleName() : null)
                                            + " " +
                                            "CardAfterIncome " + (bestCardAfterIncome != null ? bestCardAfterIncome.getClass().getSimpleName() : null)
                            );
                        }
                    }
                }
            }

        }

        final MarsGameRow marsGameRow = datasetCollectionService.collectGameAndPlayers(
                game,
                player,
                anotherPlayer
        );

        if (marsGameRow == null) {
            return 0;
        }

        addCardIncome(player, marsGameRow.getPlayer());
        addCardIncome(anotherPlayer, marsGameRow.getOpponent());


        return deepNetwork.testState(marsGameRow, player.isFirstBot() ? 1 : 2);
    }

    public float projectPlayPhase5(MarsGame game, Player player) {

        game = new MarsGame(game);

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        player = game.getPlayerByUuid(player.getUuid());
        Player anotherPlayer = players.get(0) == player
                ? players.get(1)
                : players.get(0);

        final MarsGameRow marsGameRow = datasetCollectionService.collectGameAndPlayers(
                game,
                player,
                players.get(0) == player
                        ? players.get(1)
                        : players.get(0)
        );

        if (marsGameRow == null) {
            return 0;
        }

        addDraftedCards(player, marsGameRow.getPlayer(), true, player.isFirstBot() ? 1 : 2);
        addDraftedCards(anotherPlayer, marsGameRow.getOpponent(), false, player.isFirstBot() ? 1 : 2);

        if (false) {//TODO switch back
            long badCards = aiPickCardProjectionService.countCardsWithNegativeChance(game, player);
            long goodCards = player.getHand().size() - badCards;

            if (badCards > goodCards && goodCards < 4) {
                return 1;
            }
        }


        return deepNetwork.testState(marsGameRow, player.isFirstBot() ? 1 : 2);
    }

    private void addDraftedCards(Player player, MarsPlayerRow marsPlayerRow, boolean chooseFifthPhase, int network) {
        float total = countTotalCardsToTake(player, chooseFifthPhase);

        marsPlayerRow.setCards(Math.max(0, marsPlayerRow.getCards() + total));
    }

    private float countTotalCardsToTake(Player player, boolean chooseFifthPhase) {
        int initialCardsToDraft = (chooseFifthPhase ? 5 : 2);
        int initialCardsToTake = (chooseFifthPhase ? 2 : 1);

        if (chooseFifthPhase && player.hasPhaseUpgrade(Constants.PHASE_5_UPGRADE_KEEP_EXTRA)) {
            initialCardsToDraft = 4;
            initialCardsToTake = 3;
        }

        if (chooseFifthPhase && player.hasPhaseUpgrade(Constants.PHASE_5_UPGRADE_SEE_EXTRA)) {
            initialCardsToDraft = 8;
        }

        int cardsToTake = draftCardsService.countExtraCardsToTake(player);
        int cardsToDraft = draftCardsService.countExtraCardsToDraft(player);

        return (initialCardsToTake + cardsToTake) + (initialCardsToDraft + cardsToDraft) * 0.45f;
    }

    private void addCardIncome(Player player, MarsPlayerRow marsPlayerRow) {
        marsPlayerRow.setCards(Math.max(0, marsPlayerRow.getCards() + player.getCardIncome()));
    }

    private void addMainIncome(Player player) {
        player.setMc(player.getMc() + player.getMcIncome() + player.getTerraformingRating());
        player.setHeat(player.getHeat() + player.getHeatIncome());
        player.setPlants(player.getPlants() + player.getPlantsIncome());
    }

    public MarsGame projectPlayerBuildCorporationExperiment(MarsGame game, Player player, int selectedCorporationId) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        player.setSelectedCorporationCard(selectedCorporationId);
        player.setMulligan(false);
        player.getPlayed().addCard(selectedCorporationId);

        Card card = cardService.getCard(selectedCorporationId);
        final MarsContext marsContext = marsContextProvider.provide(game, player);
        card.buildProject(marsContext);
        if (card.onBuiltEffectApplicableToItself()) {
            card.postProjectBuiltEffect(marsContext, card, aiDiscoveryDecisionService.getCorporationInput(game, player, card.getCardMetadata().getCardAction()));
        }

        return game;
    }

    public MarsGame projectOpponentCorporationBuildExperiment(MarsGame game, Player currentPlayer) {
        game = new MarsGame(game);
        game.getPlayerUuidToPlayer().values().stream().filter(player -> !player.getUuid().equals(currentPlayer.getUuid())).forEach(
                opponent -> {
                    opponent.setMulligan(false);
                    opponent.setSelectedCorporationCard(opponent.getCorporations().size());
                    opponent.setMc(50);
                }
        );
        return game;
    }


}
