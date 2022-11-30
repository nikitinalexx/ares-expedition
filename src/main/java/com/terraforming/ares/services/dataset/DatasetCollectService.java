package com.terraforming.ares.services.dataset;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.WinPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by oleksii.nikitin
 * Creation date 30.11.2022
 */
@Service
@RequiredArgsConstructor
public class DatasetCollectService {
    private final CardService cardService;
    private final WinPointsService winPointsService;
    private final DraftCardsService draftCardsService;

    public void collect(MarsGame marsGame, MarsGameDataset marsGameDataset) {
        for (int i = 0; i < 2; i++) {
            Player currentPlayer = marsGame.getPlayerByUuid(marsGameDataset.getPlayers().get(i));
            Player anotherPlayer = marsGame.getPlayerByUuid(marsGameDataset.getPlayers().get(i == 0 ? 1 : 0));
            marsGameDataset.getPlayerToRows().get(currentPlayer.getUuid()).add(
                    collectPlayerData(marsGame, currentPlayer, anotherPlayer)
            );
        }
    }

    public MarsGameRow collectPlayerData(MarsGame game, Player currentPlayer, Player anotherPlayer) {
        final Map<Tag, Long> tagOccurence = getPlayerTagsCount(cardService, currentPlayer);

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
                .space(tagOccurence.getOrDefault(Tag.SPACE, 0L))
                .earth(tagOccurence.getOrDefault(Tag.EARTH, 0L))
                .event(tagOccurence.getOrDefault(Tag.EVENT, 0L))
                .science(tagOccurence.getOrDefault(Tag.SCIENCE, 0L))
                .plant(tagOccurence.getOrDefault(Tag.PLANT, 0L))
                .energy(tagOccurence.getOrDefault(Tag.ENERGY, 0L))
                .building(tagOccurence.getOrDefault(Tag.BUILDING, 0L))
                .animal(tagOccurence.getOrDefault(Tag.ANIMAL, 0L))
                .jupiter(tagOccurence.getOrDefault(Tag.JUPITER, 0L))
                .microbe(tagOccurence.getOrDefault(Tag.MICROBE, 0L))
                .extraCardsToTake(draftCardsService.countExtraCardsToTake(currentPlayer))
                .extraCardsToSee(draftCardsService.countExtraCardsToDraft(currentPlayer))
                .cards(collectSpecificBlueCards(currentPlayer))
                .build();
    }

    private static Map<Tag, Long> getPlayerTagsCount(CardService cardService, Player player) {
        return player.getPlayed().getCards()
                .stream()
                .map(cardService::getCard)
                .flatMap(card -> card.getTags().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private static final List<Integer> cardsToLog = List.of(
            1, 2, 5, 6, 8, 17, 19, 23, 24, 25, 30, 33, 37, 39, 40, 42, 44, 45, 46, 48, 51, 52, 53, 55, 61, 217
    );

    private static List<Integer> collectSpecificBlueCards(Player player) {
        return cardsToLog.stream()
                .flatMap(cardId -> Stream.of(
                        player.getHand().containsCard(cardId) ? 1 : 0,
                        player.getPlayed().containsCard(cardId) ? 1 : 0
                ))
                .collect(Collectors.toList());
    }
}
