package com.terraforming.ares.dataset;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardFactory;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.WinPointsService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DatasetCollectionService {
    private final CardService cardService;
    private final WinPointsService winPointsService;
    private final CardFactory cardFactory;
    private final Map<Integer, Integer> corporationIdToIndex = new HashMap<>();

    public DatasetCollectionService(CardService cardService, WinPointsService winPointsService, CardFactory cardFactory) {
        this.cardService = cardService;
        this.winPointsService = winPointsService;
        this.cardFactory = cardFactory;

        List<Card> sortedBaseCorporations = cardFactory.getSortedBaseCorporations();
        Map<Integer, Card> buffedCorporationsMapping = cardFactory.getBuffedCorporationsMapping();

        for (int i = 0; i < sortedBaseCorporations.size(); i++) {
            corporationIdToIndex.put(sortedBaseCorporations.get(i).getId(), i);
        }

        for (Map.Entry<Integer, Card> entry : buffedCorporationsMapping.entrySet()) {
            int index = corporationIdToIndex.get(entry.getKey());
            corporationIdToIndex.remove(entry.getKey());

            corporationIdToIndex.put(entry.getValue().getId(), index);
        }
        System.out.println(corporationIdToIndex);
    }

    public float[] getMarsGameRowForUse(MarsGameRow marsGameRow) {
        int corporationsSize = corporationIdToIndex.size();
        float[] result = new float[43 + corporationsSize];
        int counter = 0;

        //input
        result[counter++] = marsGameRow.turn;
        result[counter++] = marsGameRow.winPoints;
        result[counter++] = marsGameRow.mcIncome;
        result[counter++] = marsGameRow.mc;
        result[counter++] = marsGameRow.steelIncome;
        result[counter++] = marsGameRow.titaniumIncome;
        result[counter++] = marsGameRow.plantsIncome;
        result[counter++] = marsGameRow.plants;
        result[counter++] = marsGameRow.heatIncome;
        result[counter++] = marsGameRow.heat;
        result[counter++] = marsGameRow.cardsIncome;
        result[counter++] = marsGameRow.cardsInHand;
        result[counter++] = marsGameRow.cardsBuilt;

        result[counter++] = marsGameRow.greenCards;
        result[counter++] = marsGameRow.redCards;

        result[counter++] = marsGameRow.heatEarthIncome;
        result[counter++] = marsGameRow.mcAnimalPlantIncome;
        result[counter++] = marsGameRow.cardScienceIncome;
        result[counter++] = marsGameRow.mcEarthIncome;
        result[counter++] = marsGameRow.plantPlantIncome;
        result[counter++] = marsGameRow.mcScienceIncome;
        result[counter++] = marsGameRow.mcTwoBuildingIncome;
        result[counter++] = marsGameRow.mcEnergyIncome;
        result[counter++] = marsGameRow.mcSpaceIncome;
        result[counter++] = marsGameRow.heatSpaceIncome;
        result[counter++] = marsGameRow.mcEventIncome;
        result[counter++] = marsGameRow.heatEnergyIncome;
        result[counter++] = marsGameRow.plantMicrobeIncome;
        result[counter++] = marsGameRow.mcForestIncome;

        result[counter++] = marsGameRow.oxygenLevel;
        result[counter++] = marsGameRow.temperatureLevel;
        result[counter++] = marsGameRow.oceansLevel;
        result[counter++] = marsGameRow.opponentWinPoints;
        result[counter++] = marsGameRow.opponentMcIncome;
        result[counter++] = marsGameRow.opponentMc;
        result[counter++] = marsGameRow.opponentSteelIncome;
        result[counter++] = marsGameRow.opponentTitaniumIncome;
        result[counter++] = marsGameRow.opponentPlantsIncome;
        result[counter++] = marsGameRow.opponentPlants;
        result[counter++] = marsGameRow.opponentHeatIncome;
        result[counter++] = marsGameRow.opponentHeat;
        result[counter++] = marsGameRow.opponentCardsIncome;
        result[counter++] = marsGameRow.opponentCardsBuilt;

        int corporationIndex = corporationIdToIndex.get(marsGameRow.getCorporationId());
        if (corporationIndex < 0 || corporationIndex >= corporationsSize) {
            throw new IllegalStateException("Invalid corporation index " + corporationIndex);
        }

        for (int i = 0; i < corporationsSize; i++) {
            result[counter++] = (corporationIndex == i ? 1 : 0);
        }


        return result;
    }

    public float[] getMarsGameRowForStudy(MarsGameRow marsGameRow) {
        int corporationsSize = corporationIdToIndex.size();
        float[] result = new float[44 + corporationsSize];
        int counter = 0;

        //input
        result[counter++] = marsGameRow.turn;
        result[counter++] = marsGameRow.winPoints;
        result[counter++] = marsGameRow.mcIncome;
        result[counter++] = marsGameRow.mc;
        result[counter++] = marsGameRow.steelIncome;
        result[counter++] = marsGameRow.titaniumIncome;
        result[counter++] = marsGameRow.plantsIncome;
        result[counter++] = marsGameRow.plants;
        result[counter++] = marsGameRow.heatIncome;
        result[counter++] = marsGameRow.heat;
        result[counter++] = marsGameRow.cardsIncome;
        result[counter++] = marsGameRow.cardsInHand;
        result[counter++] = marsGameRow.cardsBuilt;

        result[counter++] = marsGameRow.greenCards;
        result[counter++] = marsGameRow.redCards;

        result[counter++] = marsGameRow.heatEarthIncome;
        result[counter++] = marsGameRow.mcAnimalPlantIncome;
        result[counter++] = marsGameRow.cardScienceIncome;
        result[counter++] = marsGameRow.mcEarthIncome;
        result[counter++] = marsGameRow.plantPlantIncome;
        result[counter++] = marsGameRow.mcScienceIncome;
        result[counter++] = marsGameRow.mcTwoBuildingIncome;
        result[counter++] = marsGameRow.mcEnergyIncome;
        result[counter++] = marsGameRow.mcSpaceIncome;
        result[counter++] = marsGameRow.heatSpaceIncome;
        result[counter++] = marsGameRow.mcEventIncome;
        result[counter++] = marsGameRow.heatEnergyIncome;
        result[counter++] = marsGameRow.plantMicrobeIncome;
        result[counter++] = marsGameRow.mcForestIncome;

        result[counter++] = marsGameRow.oxygenLevel;
        result[counter++] = marsGameRow.temperatureLevel;
        result[counter++] = marsGameRow.oceansLevel;
        result[counter++] = marsGameRow.opponentWinPoints;
        result[counter++] = marsGameRow.opponentMcIncome;
        result[counter++] = marsGameRow.opponentMc;
        result[counter++] = marsGameRow.opponentSteelIncome;
        result[counter++] = marsGameRow.opponentTitaniumIncome;
        result[counter++] = marsGameRow.opponentPlantsIncome;
        result[counter++] = marsGameRow.opponentPlants;
        result[counter++] = marsGameRow.opponentHeatIncome;
        result[counter++] = marsGameRow.opponentHeat;
        result[counter++] = marsGameRow.opponentCardsIncome;
        result[counter++] = marsGameRow.opponentCardsBuilt;

        int corporationIndex = corporationIdToIndex.get(marsGameRow.getCorporationId());
        if (corporationIndex < 0 || corporationIndex >= corporationsSize) {
            throw new IllegalStateException("Invalid corporation index " + corporationIndex);
        }

        for (int i = 0; i < corporationsSize; i++) {
            result[counter++] = (corporationIndex == i ? 1 : 0);
        }


        //output
        result[counter++] = marsGameRow.winner;

        return result;
    }

    public void collectData(MarsGameDataset marsGameDataset, MarsGame marsGame) {
        List<String> players = marsGameDataset.getPlayers();

        for (int i = 0; i < 2; i++) {
            Player currentPlayer = marsGame.getPlayerByUuid(players.get(i));
            Player anotherPlayer = marsGame.getPlayerByUuid(players.get(i == 0 ? 1 : 0));
            MarsGameRow playerData = collectPlayerData(marsGame, currentPlayer, anotherPlayer);
            if (playerData != null) {
                marsGameDataset.getPlayerToRows().get(currentPlayer.getUuid()).add(playerData);
            }
        }
    }

    public MarsGameRow collectPlayerData(MarsGame game, Player currentPlayer, Player anotherPlayer) {
        if (game.getTurns() == 1 && currentPlayer.getSelectedCorporationCard() != null && currentPlayer.getMc() == winPointsService.cardService.getCard(currentPlayer.getSelectedCorporationCard()).getPrice()) {
            return null;
        }

        if (currentPlayer.getSelectedCorporationCard() == null || anotherPlayer.getSelectedCorporationCard() == null) {
            return null;
        }

        Map<CardAction, Integer> cardActionToCount = currentPlayer.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .map(Card::getCardMetadata)
                .filter(Objects::nonNull)
                .map(CardMetadata::getCardAction)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));

        Map<CardColor, Integer> colorToCount = currentPlayer.getHand().getCards().stream().map(cardService::getCard)
                .map(Card::getColor)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));

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
                .greenCards(colorToCount.getOrDefault(CardColor.GREEN, 0))
                .redCards(colorToCount.getOrDefault(CardColor.RED, 0))
                .heatEarthIncome(cardActionToCount.getOrDefault(CardAction.HEAT_EARTH_INCOME, 0))
                .mcAnimalPlantIncome(cardActionToCount.getOrDefault(CardAction.MC_ANIMAL_PLANT_INCOME, 0))
                .cardScienceIncome(cardActionToCount.getOrDefault(CardAction.CARD_SCIENCE_INCOME, 0))
                .mcEarthIncome(cardActionToCount.getOrDefault(CardAction.MC_EARTH_INCOME, 0))
                .plantPlantIncome(cardActionToCount.getOrDefault(CardAction.PLANT_PLANT_INCOME, 0))
                .mcScienceIncome(cardActionToCount.getOrDefault(CardAction.MC_SCIENCE_INCOME, 0))
                .mcTwoBuildingIncome(cardActionToCount.getOrDefault(CardAction.MC_2_BUILDING_INCOME, 0))
                .mcEnergyIncome(cardActionToCount.getOrDefault(CardAction.MC_ENERGY_INCOME, 0))
                .mcSpaceIncome(cardActionToCount.getOrDefault(CardAction.MC_SPACE_INCOME, 0))
                .heatSpaceIncome(cardActionToCount.getOrDefault(CardAction.HEAT_SPACE_INCOME, 0))
                .mcEventIncome(cardActionToCount.getOrDefault(CardAction.MC_EVENT_INCOME, 0))
                .heatEnergyIncome(cardActionToCount.getOrDefault(CardAction.HEAT_ENERGY_INCOME, 0))
                .plantMicrobeIncome(cardActionToCount.getOrDefault(CardAction.PLANT_MICROBE_INCOME, 0))
                .mcForestIncome(cardActionToCount.getOrDefault(CardAction.MC_FOREST_INCOME, 0))
                .oxygenLevel(game.getPlanet().getOxygenValue())
                .temperatureLevel(game.getPlanet().getTemperatureValue())
                .oceansLevel(Constants.MAX_OCEANS - game.getPlanet().oceansLeft())
                .opponentWinPoints(winPointsService.countWinPoints(anotherPlayer, game))
                .opponentMcIncome(anotherPlayer.getMcIncome() + anotherPlayer.getTerraformingRating())
                .opponentMc(anotherPlayer.getMc())
                .opponentSteelIncome(anotherPlayer.getSteelIncome())
                .opponentTitaniumIncome(anotherPlayer.getTitaniumIncome())
                .opponentPlantsIncome(anotherPlayer.getPlantsIncome())
                .opponentPlants(anotherPlayer.getPlants())
                .opponentHeatIncome(anotherPlayer.getHeatIncome())
                .opponentHeat(anotherPlayer.getHeat())
                .opponentCardsIncome(anotherPlayer.getCardIncome())
                .opponentCardsBuilt(anotherPlayer.getPlayed().size() - 1)
                .corporationId(currentPlayer.getSelectedCorporationCard())
                .build();
    }

}
