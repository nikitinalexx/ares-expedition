package com.terraforming.ares.dataset;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardFactory;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.WinPointsService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DatasetCollectionService {
    private final CardService cardService;
    private final WinPointsService winPointsService;
    private final DraftCardsService draftCardsService;

    private final Map<Integer, Integer> corporationIdToIndex = new HashMap<>();
    private final Map<Integer, Integer> blueCardIdToIndex = new HashMap<>();

    public DatasetCollectionService(CardService cardService, WinPointsService winPointsService, CardFactory cardFactory, DraftCardsService draftCardsService) {
        this.cardService = cardService;
        this.winPointsService = winPointsService;
        this.draftCardsService = draftCardsService;

        List<Card> sortedBaseCorporations = cardFactory.getSortedBaseCorporations()
                .stream()
                .filter(card -> card.getSpecialEffects() == null || !card.getSpecialEffects().contains(SpecialEffect.THARSIS_REPUBLIC))
                .collect(Collectors.toList());
        Map<Integer, Card> buffedCorporationsMapping = cardFactory.getBuffedCorporationsMapping();

        for (int i = 0; i < sortedBaseCorporations.size(); i++) {
            corporationIdToIndex.put(sortedBaseCorporations.get(i).getId(), i);
        }

        for (Map.Entry<Integer, Card> entry : buffedCorporationsMapping.entrySet()) {
            int index = corporationIdToIndex.get(entry.getKey());
            corporationIdToIndex.remove(entry.getKey());

            corporationIdToIndex.put(entry.getValue().getId(), index);
        }

        List<Integer> blueCardsForAi = cardFactory.getBlueCardsForAi();

        for (int i = 0; i < blueCardsForAi.size(); i++) {
            blueCardIdToIndex.put(blueCardsForAi.get(i), i);
        }
    }

    public float[] getMarsGameRowForUse(MarsGameRow marsGameRow) {
        int corporationsSize = corporationIdToIndex.size();
        int blueCardsSize = blueCardIdToIndex.size();
        float[] result = new float[4 + corporationsSize + 2 * (MarsPlayerRow.DATA_SIZE_NO_BLUE_CARDS + blueCardsSize)];
        int counter = 0;

        //input
        result[counter++] = marsGameRow.turn;
        result[counter++] = marsGameRow.oxygenLevel;
        result[counter++] = marsGameRow.temperatureLevel;
        result[counter++] = marsGameRow.oceansLevel;

        if (corporationIdToIndex.containsKey(marsGameRow.getCorporationId())) {
            int corporationIndex = corporationIdToIndex.get(marsGameRow.getCorporationId());
            if (corporationIndex < 0 || corporationIndex >= corporationsSize) {
                throw new IllegalStateException("Invalid corporation index " + corporationIndex);
            }
            result[counter + corporationIndex] = 1;
        }
        counter += corporationsSize;

        collectGameData(result, counter, marsGameRow.getPlayer());
        counter += MarsPlayerRow.DATA_SIZE_NO_BLUE_CARDS + blueCardsSize;

        collectGameData(result, counter, marsGameRow.getOpponent());

        return result;
    }

    private void collectGameData(float[] result, int counter, MarsPlayerRow player) {
        result[counter++] = player.winPoints;
        result[counter++] = player.mcIncome;
        result[counter++] = player.mc;
        result[counter++] = player.steelIncome;
        result[counter++] = player.titaniumIncome;
        result[counter++] = player.plantsIncome;
        result[counter++] = player.plants;
        result[counter++] = player.heatIncome;
        result[counter++] = player.heat;
        result[counter++] = player.cardsIncome;
        result[counter++] = player.cardsBuilt;

        result[counter++] = player.greenCards;
        result[counter++] = player.redCards;
        result[counter++] = player.blueCards;

        result[counter++] = player.anaerobicMicroorganisms;
        result[counter++] = player.decomposers;
        result[counter++] = player.decomposingFungus;
        result[counter++] = player.ghgProduction;
        result[counter++] = player.nitriteReducting;
        result[counter++] = player.regolithEaters;
        result[counter++] = player.selfReplicatingBacteria;

        result[counter++] = player.scienceTagsCount;

        result[counter++] = player.extraSeeCards;
        result[counter++] = player.extraTakeCards;

        result[counter++] = player.heatEarthIncome;
        result[counter++] = player.mcAnimalPlantIncome;
        result[counter++] = player.cardScienceIncome;
        result[counter++] = player.mcEarthIncome;
        result[counter++] = player.plantPlantIncome;
        result[counter++] = player.mcScienceIncome;
        result[counter++] = player.mcTwoBuildingIncome;
        result[counter++] = player.mcEnergyIncome;
        result[counter++] = player.mcSpaceIncome;
        result[counter++] = player.heatSpaceIncome;
        result[counter++] = player.mcEventIncome;
        result[counter++] = player.heatEnergyIncome;
        result[counter++] = player.plantMicrobeIncome;
        result[counter++] = player.mcForestIncome;

        for (Integer blueCard : player.getBlueCardsList()) {
            if (!blueCardIdToIndex.containsKey(blueCard)) {
                continue;
            }
            result[counter + blueCardIdToIndex.get(blueCard)] = 1;
        }
    }

    public float[] getMarsGameRowForStudy(MarsGameRow marsGameRow) {
        int corporationsSize = corporationIdToIndex.size();
        int blueCardsSize = blueCardIdToIndex.size();
        float[] result = new float[4 + corporationsSize + 2 * (MarsPlayerRow.DATA_SIZE_NO_BLUE_CARDS + blueCardsSize) + 1];
        int counter = 0;

        //input
        result[counter++] = marsGameRow.turn;
        result[counter++] = marsGameRow.oxygenLevel;
        result[counter++] = marsGameRow.temperatureLevel;
        result[counter++] = marsGameRow.oceansLevel;

        if (corporationIdToIndex.containsKey(marsGameRow.getCorporationId())) {
            int corporationIndex = corporationIdToIndex.get(marsGameRow.getCorporationId());
            if (corporationIndex < 0 || corporationIndex >= corporationsSize) {
                throw new IllegalStateException("Invalid corporation index " + corporationIndex);
            }
            result[counter + corporationIndex] = 1;
        }
        counter += corporationsSize;

        collectGameData(result, counter, marsGameRow.getPlayer());
        counter += MarsPlayerRow.DATA_SIZE_NO_BLUE_CARDS + blueCardsSize;

        collectGameData(result, counter, marsGameRow.getOpponent());
        counter += MarsPlayerRow.DATA_SIZE_NO_BLUE_CARDS + blueCardsSize;

        //output
        result[counter] = marsGameRow.winner;

        return result;
    }

    public void collectData(MarsGameDataset marsGameDataset, MarsGame marsGame) {
        List<String> players = marsGameDataset.getPlayers();

        for (int i = 0; i < 2; i++) {
            Player currentPlayer = marsGame.getPlayerByUuid(players.get(i));
            Player anotherPlayer = marsGame.getPlayerByUuid(players.get(i == 0 ? 1 : 0));
            MarsGameRow playerData = collectGameData(marsGame, currentPlayer, anotherPlayer);
            if (playerData != null) {
                marsGameDataset.getPlayerToRows().get(currentPlayer.getUuid()).add(playerData);
            }
        }
    }

    public MarsGameRow collectGameData(MarsGame game, Player currentPlayer, Player anotherPlayer) {
        if (game.getTurns() == 1 && currentPlayer.getSelectedCorporationCard() != null && currentPlayer.getMc() == winPointsService.cardService.getCard(currentPlayer.getSelectedCorporationCard()).getPrice()) {
            return null;
        }

        if (currentPlayer.getSelectedCorporationCard() == null || anotherPlayer.getSelectedCorporationCard() == null) {
            return null;
        }

        return MarsGameRow.builder()
                .turn(game.getTurns())
                .oxygenLevel(game.getPlanet().getOxygenValue())
                .temperatureLevel(game.getPlanet().getTemperatureValue())
                .oceansLevel(Constants.MAX_OCEANS - game.getPlanet().oceansLeft())
                .player(collectPlayerData(game, currentPlayer))
                .opponent(collectPlayerData(game, anotherPlayer))
                .corporationId(currentPlayer.getSelectedCorporationCard())
                .build();
    }

    private MarsPlayerRow collectPlayerData(MarsGame game, Player currentPlayer) {
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

        List<Integer> blueCardsList = currentPlayer.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getColor() == CardColor.BLUE)
                .map(Card::getId)
                .collect(Collectors.toList());

        return MarsPlayerRow.builder()
                .winPoints(winPointsService.countWinPointsWithFloats(currentPlayer, game))
                .mcIncome(currentPlayer.getMcIncome() + currentPlayer.getTerraformingRating())
                .mc(currentPlayer.getMc())
                .steelIncome(currentPlayer.getSteelIncome())
                .titaniumIncome(currentPlayer.getTitaniumIncome())
                .plantsIncome(currentPlayer.getPlantsIncome())
                .plants(currentPlayer.getPlants())
                .heatIncome(currentPlayer.getHeatIncome())
                .heat(currentPlayer.getHeat())
                .cardsIncome(currentPlayer.getCardIncome())
                .cardsBuilt(currentPlayer.getPlayed().size() - 1)
                .extraSeeCards(draftCardsService.countExtraCardsToDraft(currentPlayer))
                .extraTakeCards(draftCardsService.countExtraCardsToTake(currentPlayer))
                .greenCards(colorToCount.getOrDefault(CardColor.GREEN, 0))
                .redCards(colorToCount.getOrDefault(CardColor.RED, 0))
                .blueCards(colorToCount.getOrDefault(CardColor.BLUE, 0))
                .anaerobicMicroorganisms(currentPlayer.getCardResourcesCount().getOrDefault(AnaerobicMicroorganisms.class, 0))
                .decomposers(currentPlayer.getCardResourcesCount().getOrDefault(Decomposers.class, 0))
                .decomposingFungus(currentPlayer.getCardResourcesCount().getOrDefault(DecomposingFungus.class, 0))
                .ghgProduction(currentPlayer.getCardResourcesCount().getOrDefault(GhgProductionBacteria.class, 0))
                .nitriteReducting(currentPlayer.getCardResourcesCount().getOrDefault(NitriteReductingBacteria.class, 0))
                .regolithEaters(currentPlayer.getCardResourcesCount().getOrDefault(RegolithEaters.class, 0))
                .selfReplicatingBacteria(currentPlayer.getCardResourcesCount().getOrDefault(SelfReplicatingBacteria.class, 0))
                .scienceTagsCount(cardService.countPlayedTags(currentPlayer, Set.of(Tag.SCIENCE)))
                .blueCardsList(blueCardsList)
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
                .build();
    }

}
