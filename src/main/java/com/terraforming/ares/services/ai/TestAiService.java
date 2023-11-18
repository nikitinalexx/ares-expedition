package com.terraforming.ares.services.ai;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.dataset.MarsPlayerRow;
import com.terraforming.ares.mars.MarsGame;
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

        if (colors.contains(CardColor.RED) || colors.contains(CardColor.BLUE)) {
            player.setBuilds(List.of(new BuildDto(BuildType.BLUE_RED), new BuildDto(BuildType.BLUE_RED)));
            BuildProjectPrediction redProject = aiBuildProjectService.getBestProjectToBuildSecondPhase(game, player, Set.of(CardColor.RED, CardColor.BLUE), ProjectionStrategy.FROM_PHASE);
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

        final MarsGameRow marsGameRow = datasetCollectionService.collectGameData(
                game,
                player,
                players.get(0) == player
                        ? players.get(1)
                        : players.get(0)
        );

        if (marsGameRow == null) {
            return 0;
        }

        addCardIncome(player, marsGameRow.getPlayer());
        addCardIncome(anotherPlayer, marsGameRow.getOpponent());


        return deepNetwork.testState(marsGameRow, player.isFirstBot() ? 1 : 2);
    }

    private void addCardIncome(Player player, MarsPlayerRow marsPlayerRow) {
        marsPlayerRow.setGreenCards(Math.max(0, marsPlayerRow.getGreenCards() + player.getCardIncome() * Constants.GREEN_CARDS_RATIO));
        marsPlayerRow.setRedCards(Math.max(0, marsPlayerRow.getRedCards() + player.getCardIncome() * Constants.RED_CARDS_RATIO));
        marsPlayerRow.setBlueCards(Math.max(0, marsPlayerRow.getBlueCards() + player.getCardIncome() * Constants.BLUE_CARDS_RATIO));
    }

    private void addMainIncome(Player player) {
        player.setMc(player.getMc() + player.getMcIncome() + player.getTerraformingRating());
        player.setHeat(player.getHeat() + player.getHeatIncome());
        player.setPlants(player.getPlants() + player.getPlantsIncome());
    }


}
