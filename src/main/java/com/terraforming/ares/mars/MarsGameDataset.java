package com.terraforming.ares.mars;

import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.WinPointsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 26.11.2022
 */
public class MarsGameDataset {
    private final List<String> players;
    private final Map<String, List<MarsGameRow>> playerToRows = new HashMap<>();

    public MarsGameDataset(Map<String, Player> playerUuidToPlayer) {
        this.players = new ArrayList<>(playerUuidToPlayer.keySet());
        if (this.players.size() != 2) {
            throw new IllegalStateException("Only 2 players supported for a model so far");
        }
        playerUuidToPlayer.keySet().forEach(playerUuid -> playerToRows.put(playerUuid, new ArrayList<>()));
    }

    public void collectData(WinPointsService winPointsService, MarsGame marsGame) {
        for (int i = 0; i < 2; i++) {
            Player currentPlayer = marsGame.getPlayerByUuid(players.get(i));
            Player anotherPlayer = marsGame.getPlayerByUuid(players.get(i == 0 ? 1 : 0));
            playerToRows.get(currentPlayer.getUuid()).add(collectPlayerData(winPointsService, marsGame, currentPlayer, anotherPlayer));
        }
    }

    public void markWinner(String player) {
        playerToRows.forEach(
                (p, data) -> {
                    int result = player.equals(p) ? 1 : 0;
                    data.forEach(dataRow -> dataRow.setWinner(result));
                }
        );
    }

    public List<MarsGameRow> getFirstPlayerRows() {
        return playerToRows.get(players.get(0));
    }

    public List<MarsGameRow> getSecondPlayerRows() {
        return playerToRows.get(players.get(1));
    }

    public static MarsGameRow collectPlayerData(WinPointsService winPointsService, MarsGame game, Player currentPlayer, Player anotherPlayer) {
        return MarsGameRow.builder()
                .turn(game.getTurns())
                .winPoints(winPointsService.countWinPoints(currentPlayer, game))
                .mcIncome(currentPlayer.getMcIncome() + currentPlayer.getTerraformingRating())
                .mc(currentPlayer.getMc())
                .steelIncome(currentPlayer.getSteelIncome())
                .titaniumIncome(currentPlayer.getTitaniumIncome())
                .plantsIncome(currentPlayer.getPlantsIncome())
                .plants(currentPlayer.getPlants())
                .heatIncome(currentPlayer.getHeatIncome())
                .heat(currentPlayer.getHeat())
                .cardsIncome(currentPlayer.getCardIncome())
                .cardsInHand(currentPlayer.getHand().size())
                .cardsBuilt(currentPlayer.getPlayed().size() - 1)
                .oxygenLevel(game.getPlanet().getOxygenValue())
                .temperatureLevel(game.getPlanet().getTemperatureValue())
                .oceansLevel(Constants.MAX_OCEANS - game.getPlanet().oceansLeft())
                .opponentWinPoints(winPointsService.countWinPoints(anotherPlayer, game))
                .opponentMcIncome(anotherPlayer.getMcIncome())
                .opponentMc(anotherPlayer.getMc())
                .opponentSteelIncome(anotherPlayer.getSteelIncome())
                .opponentTitaniumIncome(anotherPlayer.getTitaniumIncome())
                .opponentPlantsIncome(anotherPlayer.getPlantsIncome())
                .opponentPlants(anotherPlayer.getPlants())
                .opponentHeatIncome(anotherPlayer.getHeatIncome())
                .opponentHeat(anotherPlayer.getHeat())
                .opponentCardsIncome(anotherPlayer.getCardIncome())
                .opponentCardsBuilt(anotherPlayer.getPlayed().size() - 1)
                .build();
    }

}
