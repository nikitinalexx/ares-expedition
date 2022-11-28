package com.terraforming.ares.mars;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardFactory;
import com.terraforming.ares.services.WinPointsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public void collectData(CardFactory cardFactory, WinPointsService winPointsService, MarsGame marsGame) {
        for (int i = 0; i < 2; i++) {
            Player currentPlayer = marsGame.getPlayerByUuid(players.get(i));
            Player anotherPlayer = marsGame.getPlayerByUuid(players.get(i == 0 ? 1 : 0));
            playerToRows.get(currentPlayer.getUuid()).add(collectPlayerData(cardFactory, winPointsService, marsGame, currentPlayer, anotherPlayer));
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

    public static MarsGameRow collectPlayerData(CardFactory cardFactory, WinPointsService winPointsService, MarsGame game, Player currentPlayer, Player anotherPlayer) {
        return MarsGameRow.builder()
                .turn(game.getTurns())
                .winPoints(winPointsService.countWinPoints(currentPlayer, game))
                .mcIncome(currentPlayer.getMcIncome())
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
                .cards(collectAllCards(currentPlayer, cardFactory))
                .build();
    }

    private static List<Integer> collectAllCards(Player player, CardFactory cardFactory) {
        return cardFactory.getSortedProjects().stream()
                .map(Card::getId)
                .map(cardId -> {
                    if (player.getHand().containsCard(cardId)) {
                        return 1;
                    } else if (player.getPlayed().containsCard(cardId)) {
                        return 2;
                    } else {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
    }

    private static List<Integer> collectBlueCards(Player player, CardFactory cardFactory) {
        return cardFactory.getSortedBlueCards().stream()
                .map(
                        Card::getId
                )
                .map(cardId -> {
                    if (player.getHand().containsCard(cardId)) {
                        return 1;
                    } else if (player.getPlayed().containsCard(cardId)) {
                        return 2;
                    } else {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
    }

}
