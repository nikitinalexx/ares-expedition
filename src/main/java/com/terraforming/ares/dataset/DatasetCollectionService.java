package com.terraforming.ares.dataset;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.corporations.*;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.awards.AbstractAward;
import com.terraforming.ares.model.awards.BaseAward;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.services.CardFactory;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.WinPointsService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DatasetCollectionService {
    private final CardService cardService;
    private final WinPointsService winPointsService;
    private final DraftCardsService draftCardsService;
    private final DynamicCardEffectService dynamicCardEffectService;

    private final Map<Integer, Integer> corporationIdToIndex = new HashMap<>();
    private final Map<Integer, Integer> blueCardIdToIndex = new HashMap<>();

    //TODO consider Austellar CORP
    private final List<Class<?>> CORPORATIONS_WITH_REPEATED_EFFECT = List.of(TharsisCorporation.class, Inventrix.class, ExocorpCorporation.class, ApolloIndustriesCorporation.class, AustellarCorporation.class, NebuLabsCorporation.class, InterplanetaryCinematics.class);

    public DatasetCollectionService(CardService cardService, WinPointsService winPointsService, CardFactory cardFactory, DraftCardsService draftCardsService, DynamicCardEffectService dynamicCardEffectService) {
        this.cardService = cardService;
        this.winPointsService = winPointsService;
        this.draftCardsService = draftCardsService;
        this.dynamicCardEffectService = dynamicCardEffectService;

        List<Card> sortedCorporations = Stream
                .concat(cardFactory.getSortedBaseCorporations().stream(), cardFactory.getSortedDiscoveryCorporations().stream())
                .filter(card -> !CORPORATIONS_WITH_REPEATED_EFFECT.contains(card.getClass()))
                .sorted(Comparator.comparing(Card::getId))
                .collect(Collectors.toList());

        Map<Integer, Card> buffedCorporationsMapping = cardFactory.getBuffedCorporationsMapping();

        for (int i = 0; i < sortedCorporations.size(); i++) {
            corporationIdToIndex.put(sortedCorporations.get(i).getId(), i);
        }

        for (Map.Entry<Integer, Card> entry : buffedCorporationsMapping.entrySet()) {
            if (!corporationIdToIndex.containsKey(entry.getKey())) {
                continue;
            }
            int index = corporationIdToIndex.get(entry.getKey());
            corporationIdToIndex.remove(entry.getKey());

            corporationIdToIndex.put(entry.getValue().getId(), index);
        }

        List<Integer> blueCardsForAi = cardFactory.getBlueCardsForAi();

        for (int i = 0; i < blueCardsForAi.size(); i++) {
            blueCardIdToIndex.put(blueCardsForAi.get(i), i);
        }
    }

    private void collectGameAndPlayers(float[] result, int counter, MarsPlayerRow player) {
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
        result[counter++] = player.extraSeeCards;
        result[counter++] = player.extraTakeCards;

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
        result[counter++] = player.fibrousCompositeScience;

        result[counter++] = player.scienceTagsCount;
        result[counter++] = player.jupiterTagsCount;

        result[counter++] = player.amplifyEffect;
        result[counter++] = player.cardsPriceEffect;
        result[counter++] = player.cardForScienceEffect;
        result[counter++] = player.phaseRevealBonus;
        result[counter++] = player.eventDiscount;
        result[counter++] = player.cardDiscount;
        result[counter++] = player.cardPerEvent;
        result[counter++] = player.wpPerJupiter;

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

        for (int dynamicEffect : player.dynamicEffects) {
            result[counter++] = dynamicEffect;
        }

        for (int type1Upgrade: player.type1Upgrades) {
            result[counter++] = type1Upgrade;
        }

        for (int type2Upgrade: player.type2Upgrades) {
            result[counter++] = type2Upgrade;
        }

        for (Integer blueCard : player.getBlueCardsList()) {
            if (!blueCardIdToIndex.containsKey(blueCard)) {
                continue;
            }
            result[counter + blueCardIdToIndex.get(blueCard)] = 1;
        }
    }

    public float[] mapMarsGameToArrayForUse(MarsGameRow marsGameRow) {
        return mapMarsGameToArray(marsGameRow, true);
    }

    public float[] mapMarsGameToArrayForStudy(MarsGameRow marsGameRow) {
        return mapMarsGameToArray(marsGameRow, false);
    }

    private float[] mapMarsGameToArray(MarsGameRow marsGameRow, boolean forUse) {
        int corporationsSize = corporationIdToIndex.size();
        int blueCardsSize = blueCardIdToIndex.size();
        float[] result = new float[4 + //turn and global parameters
                corporationsSize +
                2 * (MarsPlayerRow.DATA_SIZE_NO_BLUE_CARDS + blueCardsSize) +
                (forUse ? 0 : 1) + //won or lose
                marsGameRow.awards.length + marsGameRow.milestones.length
                ];
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

        collectGameAndPlayers(result, counter, marsGameRow.getPlayer());
        counter += MarsPlayerRow.DATA_SIZE_NO_BLUE_CARDS + blueCardsSize;

        collectGameAndPlayers(result, counter, marsGameRow.getOpponent());
        counter += MarsPlayerRow.DATA_SIZE_NO_BLUE_CARDS + blueCardsSize;

        for (int i = 0; i < marsGameRow.awards.length; i++) {
            result[counter++] = marsGameRow.awards[i];
        }

        for (int i = 0; i < marsGameRow.milestones.length; i++) {
            result[counter++] = marsGameRow.milestones[i];
        }

        if (!forUse) {
            //output
            result[counter++] = marsGameRow.winner;
        }

        if (counter != result.length) {
            throw new IllegalStateException("Not all parameters were saved");
        }

        return result;
    }

    public void collectData(MarsGameDataset marsGameDataset, MarsGame marsGame) {
        List<String> players = marsGameDataset.getPlayers();

        for (int i = 0; i < 2; i++) {
            Player currentPlayer = marsGame.getPlayerByUuid(players.get(i));
            Player anotherPlayer = marsGame.getPlayerByUuid(players.get(i == 0 ? 1 : 0));

            if (marsGame.getTurns() == 1 && currentPlayer.getSelectedCorporationCard() != null && currentPlayer.getMc() == winPointsService.cardService.getCard(currentPlayer.getSelectedCorporationCard()).getPrice()) {
                return;
            }
            MarsGameRow playerData = collectGameAndPlayers(marsGame, currentPlayer, anotherPlayer);
            if (playerData != null) {
                marsGameDataset.getPlayerToRows().get(currentPlayer.getUuid()).add(playerData);
            }
        }
    }

    public MarsGameRow collectGameAndPlayers(MarsGame game, Player currentPlayer, Player anotherPlayer) {
        if (currentPlayer.getSelectedCorporationCard() == null || anotherPlayer.getSelectedCorporationCard() == null) {
            return null;
        }

        //TODO check if it actually works
        List<Milestone> milestones = game.getMilestones();
        float[] milestonesResult = new float[milestones.size()];
        for (int i = 0; i < milestones.size(); i++) {
            Milestone milestone = milestones.get(i);
            if (!milestone.isAchieved()) {
                milestonesResult[i] = ((float) milestone.getValue(currentPlayer, cardService) / milestone.getMaxValue()) - ((float) milestone.getValue(anotherPlayer, cardService) / milestone.getMaxValue());
            }
        }

        //TODO check if it works, because the parameter has no cap
        List<BaseAward> awards = game.getAwards();
        float[] awardsResult = new float[awards.size()];
        for (int i = 0; i < awards.size(); i++) {
            AbstractAward award = (AbstractAward) awards.get(i);
            awardsResult[i] = ((float) award.comparableParamExtractor(cardService).applyAsInt(currentPlayer) / award.getMaxValue()) - ((float) award.comparableParamExtractor(cardService).applyAsInt(anotherPlayer) / award.getMaxValue());
        }

        return MarsGameRow.builder()
                .turn(game.getTurns())
                .milestones(milestonesResult)
                .awards(awardsResult)
                .oxygenLevel(game.getPlanet().getOxygenValue())
                .temperatureLevel(game.getPlanet().getTemperatureValue() - game.getPlanet().getMinimumTemperature())
                .oceansLevel(Constants.MAX_OCEANS - game.getPlanet().oceansLeft())
                .player(collectPlayerData(game, currentPlayer))
                .opponent(collectPlayerData(game, anotherPlayer))
                .corporationId(currentPlayer.getSelectedCorporationCard())
                .build();
    }

    private MarsPlayerRow collectPlayerData(MarsGame game, Player currentPlayer) {
        List<Card> playedCards = currentPlayer.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .collect(Collectors.toList());

        Map<CardAction, Integer> cardActionToCount = playedCards.stream()
                .map(Card::getCardMetadata)
                .filter(Objects::nonNull)
                .map(CardMetadata::getCardAction)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));

        Map<CardColor, Integer> colorToCount = playedCards.stream()
                .map(Card::getColor)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));

        List<Integer> blueCardsList = playedCards.stream()
                .filter(card -> card.getColor() == CardColor.BLUE)
                .map(Card::getId)
                .collect(Collectors.toList());

        Set<Class<?>> cardTypes = currentPlayer.getPlayed().getCards().stream().map(cardService::getCard).map(Card::getClass).collect(Collectors.toSet());

        int[] type1Upgrades = new int[5];
        int[] type2Upgrades = new int[5];

        for (int i = 0; i < 5; i++) {
            Integer upgrade = currentPlayer.getPhaseCards().get(i);
            if (upgrade == 1) {
                type1Upgrades[i] = 1;
            } else if (upgrade == 2) {
                type2Upgrades[i] = 1;
            }
        }

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
                .fibrousCompositeScience(currentPlayer.getCardResourcesCount().getOrDefault(FibrousCompositeMaterial.class, 0))

                .scienceTagsCount(cardService.countPlayedTags(currentPlayer, Set.of(Tag.SCIENCE)))
                .jupiterTagsCount(cardService.countPlayedTags(currentPlayer, Set.of(Tag.JUPITER)))

                .amplifyEffect(CardEffect.AMPLIFY.getEffectSize(cardTypes))
                .cardsPriceEffect(CardEffect.CARDS_PRICE.getEffectSize(cardTypes))
                .cardForScienceEffect(CardEffect.CARD_FOR_SCIENCE.getEffectSize(cardTypes))
                .phaseRevealBonus(CardEffect.PHASE_REVEAL_BONUS.getEffectSize(cardTypes))
                .eventDiscount(CardEffect.EVENT_DISCOUNT.getEffectSize(cardTypes))
                .cardDiscount(CardEffect.CARD_DISCOUNT.getEffectSize(cardTypes))
                .cardPerEvent(CardEffect.CARD_PER_EVENT.getEffectSize(cardTypes))
                .wpPerJupiter(CardEffect.WP_PER_JUPITER.getEffectSize(cardTypes))

                .dynamicEffects(
                        dynamicCardEffectService.getAllDynamicEffects(game, currentPlayer).stream()
                                .mapToInt(Integer::intValue)
                                .toArray()
                )

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
                .type1Upgrades(type1Upgrades)
                .type2Upgrades(type2Upgrades)
                .build();
    }

}
