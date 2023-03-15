package com.terraforming.ares.services.ai;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.dataset.MarsGameDataset;
import com.terraforming.ares.dataset.MarsGameRow;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.WinPointsService;
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
    private static final float RED_CARDS_RATIO = 40f / 151;
    private static final float GREEN_CARDS_RATIO = 111f / 151;

    private final AiBuildProjectService aiBuildProjectService;
    private final CardService cardService;
    private final WinPointsService winPointsService;
    private final DeepNetwork deepNetwork;
    private final StandardProjectService standardProjectService;
    private final AiTurnService aiTurnService;
    private final DatasetCollectionService datasetCollectionService;


    public BuildProjectPrediction getBestCardToBuild(MarsGame game, Player player, Set<CardColor> colors) {
        game = new MarsGame(game);

        player = game.getPlayerByUuid(player.getUuid());

        BuildProjectPrediction bestProject = BuildProjectPrediction.builder().build();

        if (colors.contains(CardColor.GREEN)) {
            player.setBuilds(List.of(new BuildDto(BuildType.GREEN, 3)));
            BuildProjectPrediction greenProject = aiBuildProjectService.getBestProjectToBuild(game, player, Set.of(CardColor.GREEN), ProjectionStrategy.FROM_PHASE);
            if (Constants.LOG_NET_COMPARISON && greenProject.isCanBuild()) {
                System.out.println("Best GREEN " + (greenProject.getCard().getClass().getSimpleName()) + " " + greenProject.getExpectedValue());
            }
            bestProject = greenProject;
        }

        if (colors.contains(CardColor.RED)) {
            player.setBuilds(List.of(new BuildDto(BuildType.BLUE_RED), new BuildDto(BuildType.BLUE_RED)));
            BuildProjectPrediction redProject = aiBuildProjectService.getBestProjectToBuildSecondPhase(game, player, Set.of(CardColor.RED), ProjectionStrategy.FROM_PHASE);
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

    public Integer getBestCard(MarsGame game, String playerUuid, List<Integer> cards) {

        game = new MarsGame(game);

        Player player = game.getPlayerByUuid(playerUuid);

        float bestChance = deepNetwork.testState(game, player);


        Card bestCard = null;

        for (Integer cardId : cards) {
            Card card = cardService.getCard(cardId);
            if (card.getColor() == CardColor.GREEN) {
                player.setBuilds(List.of(new BuildDto(BuildType.GREEN, 3)));
            } else {
                player.setBuilds(List.of(new BuildDto(BuildType.BLUE_RED)));
            }

            MarsGame stateAfterPlayingTheCard = aiBuildProjectService.projectBuildCardNoRequirements(game, player, card);

            if (stateAfterPlayingTheCard.getPlayerByUuid(playerUuid).getMc() < 0) {
                stateAfterPlayingTheCard.getPlayerByUuid(playerUuid).setMc(0);
            }

            float projectedChance = deepNetwork.testState(stateAfterPlayingTheCard, stateAfterPlayingTheCard.getPlayerByUuid(player.getUuid()));

            if (projectedChance > bestChance) {
                bestChance = projectedChance;
                bestCard = card;
            }
        }

        return (bestCard != null ? bestCard.getId() : null);
    }

    public float projectPlayStandardAction(MarsGame game, String playerUuid, int type) {

        game = new MarsGame(game);

        Player player = game.getPlayerByUuid(playerUuid);

        int mc = player.getMc();

        if (type == 1 && mc > standardProjectService.getProjectPrice(player, StandardProjectType.OCEAN)) {
            aiTurnService.standardProjectTurn(game, player, StandardProjectType.OCEAN);
            return deepNetwork.testState(game, player);
        }

        if (type == 2 && mc > standardProjectService.getProjectPrice(player, StandardProjectType.FOREST)) {
            aiTurnService.standardProjectTurn(game, player, StandardProjectType.FOREST);
            return deepNetwork.testState(game, player);
        }

        if (type == 3 && mc > standardProjectService.getProjectPrice(player, StandardProjectType.TEMPERATURE)) {
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

        addMainIncome(player);
        addMainIncome(anotherPlayer);

        player.setMc(player.getMc() + 4);

        final MarsGameRow marsGameRow = datasetCollectionService.collectPlayerData(
                game,
                player,
                players.get(0) == player
                        ? players.get(1)
                        : players.get(0)
        );

        if (marsGameRow == null) {
            return 0;
        }

        int cardIncome = player.getCardIncome() - anotherPlayer.getCardIncome();

        float greenCardsIncome = cardIncome * GREEN_CARDS_RATIO;
        float redCardsIncome = cardIncome * RED_CARDS_RATIO;

        marsGameRow.setGreenCards(Math.max(0, marsGameRow.getGreenCards() + greenCardsIncome));
        marsGameRow.setRedCards(Math.max(0, marsGameRow.getRedCards() + redCardsIncome));


        return deepNetwork.testState(marsGameRow, 2);
    }

    private void addMainIncome(Player player) {
        player.setMc(player.getMc() + player.getMcIncome() + player.getTerraformingRating());
        player.setHeat(player.getHeat() + player.getHeatIncome());
        player.setPlants(player.getPlants() + player.getPlantsIncome());
    }


}
