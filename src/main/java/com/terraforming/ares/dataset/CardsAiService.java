package com.terraforming.ares.dataset;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.Microprocessors;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.winPoints.WinPointsInfo;
import com.terraforming.ares.services.CardFactory;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.DeepNetwork;
import deepnetts.net.FeedForwardNetwork;
import deepnetts.util.FileIO;
import deepnetts.util.Tensor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CardsAiService {
    private static final float[] MAX_INPUTS_1 = new float[]{0.9992f, 0.9814f, 0.9977f, 0.9874f, 0.9989f, 0.9994f, 0.9984f, 0.9977f, 0.993f, 0.9993f, 0.9963f, 0.9974f, 0.9987f, 0.9997f, 0.9909f, 0.996f, 0.9977f, 0.9976f, 0.9984f, 0.9946f, 0.9929f, 0.9961f, 0.9884f, 0.9989f, 0.9997f, 0.9975f, 0.9962f, 0.9969f, 0.9966f, 0.9827f, 0.9964f, 0.9989f, 47.0f, 137.0f, 119.0f, 334.0f, 14.0f, 12.0f, 25.0f, 79.0f, 61.0f, 216.0f, 11.0f, 62.0f, 5.0f, 4.0f, 22.0f, 14.0f, 16.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 23.0f, 15.0f, 18.0f, 21.0f, 13.0f, 16.0f, 28.0f, 6.0f, 10.0f, 11.0f, 14.0f, 30.0f, 9.0f, 137.0f, 119.0f, 334.0f, 14.0f, 12.0f, 25.0f, 79.0f, 61.0f, 216.0f, 11.0f, 61.0f, 5.0f, 4.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};

    private static final float[] MAX_INPUTS_2 = new float[]{0.9992f, 0.9814f, 0.9977f, 0.9874f, 0.9989f, 0.9994f, 0.9984f, 0.9977f, 0.993f, 0.9993f, 0.9963f, 0.9974f, 0.9987f, 0.9997f, 0.9909f, 0.996f, 0.9977f, 0.9976f, 0.9984f, 0.9946f, 0.9929f, 0.9961f, 0.9884f, 0.9989f, 0.9997f, 0.9975f, 0.9962f, 0.9969f, 0.9966f, 0.9827f, 0.9964f, 0.9989f, 48.0f, 127.0f, 120.0f, 355.0f, 12.0f, 13.0f, 33.0f, 95.0f, 63.0f, 213.0f, 11.0f, 61.0f, 5.0f, 4.0f, 16.0f, 9.0f, 13.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 24.0f, 15.0f, 21.0f, 19.0f, 14.0f, 15.0f, 28.0f, 7.0f, 9.0f, 12.0f, 14.0f, 30.0f, 9.0f, 124.0f, 120.0f, 355.0f, 12.0f, 12.0f, 33.0f, 91.0f, 62.0f, 213.0f, 11.0f, 61.0f, 5.0f, 4.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};


    private static final Tensor MAX_INPUTS_TENSOR_1 = new Tensor(MAX_INPUTS_1);
    private static final Tensor MAX_INPUTS_TENSOR_2 = new Tensor(MAX_INPUTS_2);


    private final CardFactory cardFactory;
    private final WinPointsService winPointsService;
    private final CardService cardService;
    private final DeepNetwork deepNetwork;

    private final DraftCardsService draftCardsService;
    private final Map<String, List<MarsCardRow>> data = new ConcurrentHashMap<>();

    private final float[][] embeddedDenseVector;

    private ThreadLocal<FeedForwardNetwork> networkThreadLocal1;
    private ThreadLocal<FeedForwardNetwork> networkThreadLocal2;


    private final Map<Integer, Integer> blueCardIdToIndex = new HashMap<>();

    public CardsAiService(CardFactory cardFactory,
                          WinPointsService winPointsService,
                          CardService cardService,
                          DraftCardsService draftCardsService,
                          DeepNetwork deepNetwork) throws IOException, ClassNotFoundException {
        this.cardFactory = cardFactory;
        this.winPointsService = winPointsService;
        this.cardService = cardService;
        this.draftCardsService = draftCardsService;
        this.deepNetwork = deepNetwork;


        int allCardsSize = cardFactory.getAllCardIdToIndex().size();
        embeddedDenseVector = new float[allCardsSize][];

        Random random = new Random(1678834079073L);

        for (int i = 0; i < allCardsSize; i++) {
            float[] matrix = new float[32];
            for (int j = 0; j < matrix.length; j++) {
                matrix[j] = random.nextFloat();
            }
            embeddedDenseVector[i] = matrix;
        }

        networkThreadLocal1 = ThreadLocal.withInitial(
                () -> {
                    try {
                        return FileIO.createFromFile("cards1.dnet", FeedForwardNetwork.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        networkThreadLocal2 = ThreadLocal.withInitial(
                () -> {
                    try {
                        return FileIO.createFromFile("cards2.dnet", FeedForwardNetwork.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        List<Integer> blueCardsForAi = cardFactory.getBlueCardsForAi();

        for (int i = 0; i < blueCardsForAi.size(); i++) {
            blueCardIdToIndex.put(blueCardsForAi.get(i), i);
        }
    }

    public void collectData(MarsGame game, Player currentPlayer, int card) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        Player anotherPlayer = players.get(0) == currentPlayer
                ? players.get(1)
                : players.get(0);

        Map<Integer, Integer> allCardToIndex = cardFactory.getAllCardIdToIndex();

        MarsCardRow marsCardRow = getMarsCardRow(game, allCardToIndex, card, currentPlayer, anotherPlayer);

        data.compute(currentPlayer.getUuid(), (key, value) -> {
            if (value == null) {
                value = new ArrayList<>();
            }
            value.add(marsCardRow);
            return value;
        });
    }

    public Integer getBestCard(MarsGame game, String playerUuid, List<Integer> cards) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        Player currentPlayer = game.getPlayerByUuid(playerUuid);
        Player anotherPlayer = players.get(0) == game.getPlayerByUuid(playerUuid)
                ? players.get(1)
                : players.get(0);

        Map<Integer, Integer> allCardToIndex = cardFactory.getAllCardIdToIndex();


        float bestChance = -1;
        Integer bestCardId = null;

        for (Integer cardId : cards) {
            MarsCardRow marsCardRow = getMarsCardRow(game, allCardToIndex, cardId, currentPlayer, anotherPlayer);

            float newState = testState(marsCardRow, currentPlayer);
            if (newState > bestChance) {
                bestChance = newState;
                bestCardId = cardId;
            }
        }

        return bestCardId;
    }

    public Integer getWorstCard(MarsGame game, String playerUuid, List<Integer> cards, boolean canBeNull) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        Player currentPlayer = game.getPlayerByUuid(playerUuid);
        Player anotherPlayer = players.get(0) == game.getPlayerByUuid(playerUuid)
                ? players.get(1)
                : players.get(0);

        Map<Integer, Integer> allCardToIndex = cardFactory.getAllCardIdToIndex();


        float worstChance = (canBeNull ? deepNetwork.testState(game, game.getPlayerByUuid(playerUuid)) : Float.MAX_VALUE);
        Integer worstCardId = null;

        for (Integer cardId : cards) {
            MarsCardRow marsCardRow = getMarsCardRow(game, allCardToIndex, cardId, currentPlayer, anotherPlayer);

            float newState = testState(marsCardRow, currentPlayer);
            if (newState < worstChance) {
                worstChance = newState;
                worstCardId = cardId;
            }
        }

        return worstCardId;
    }

    public Integer getWorstCard(MarsGame game, String playerUuid, List<Integer> cards, float startChance) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        Player currentPlayer = game.getPlayerByUuid(playerUuid);
        Player anotherPlayer = players.get(0) == game.getPlayerByUuid(playerUuid)
                ? players.get(1)
                : players.get(0);

        Map<Integer, Integer> allCardToIndex = cardFactory.getAllCardIdToIndex();


        float worstChance = startChance;
        Integer worstCardId = null;

        for (Integer cardId : cards) {
            MarsCardRow marsCardRow = getMarsCardRow(game, allCardToIndex, cardId, currentPlayer, anotherPlayer);

            float newState = testState(marsCardRow, currentPlayer);
            if (newState < worstChance) {
                worstChance = newState;
                worstCardId = cardId;
            }
        }

        return worstCardId;
    }

    public float getCardChance(MarsGame game, String playerUuid, Integer card) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        Player currentPlayer = game.getPlayerByUuid(playerUuid);
        Player anotherPlayer = players.get(0) == game.getPlayerByUuid(playerUuid)
                ? players.get(1)
                : players.get(0);

        Map<Integer, Integer> allCardToIndex = cardFactory.getAllCardIdToIndex();


        return testState(getMarsCardRow(game, allCardToIndex, card, currentPlayer, anotherPlayer), currentPlayer);
    }

    private MarsCardAiData createCardAiData(Player player, Card card) {
        CardMetadata cardMetadata = card.getCardMetadata();

        MarsCardAiData.MarsCardAiDataBuilder builder = MarsCardAiData.builder()
                .price(card.getPrice())
                .spaceTags(cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.SPACE)))
                .earthTags(cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.EARTH)))
                .eventTags(cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.EVENT)))
                .scienceTags(cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.SCIENCE)))
                .plantTags(cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.PLANT)))
                .energyTags(cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.ENERGY)))
                .buildingTags(cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.BUILDING)))
                .animalTags(cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.ANIMAL)))
                .jupiterTags(cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.JUPITER)))
                .microbeTags(cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.MICROBE)));

        if (cardMetadata != null) {
            List<Gain> incomes = cardMetadata.getIncomes();
            if (!CollectionUtils.isEmpty(incomes)) {
                Map<GainType, Gain> typeToGain = incomes.stream().collect(Collectors.toMap(Gain::getType, Function.identity()));
                builder.mcIncome(typeToGain.getOrDefault(GainType.MC, Gain.NONE).getValue())
                        .cardIncome(typeToGain.getOrDefault(GainType.CARD, Gain.NONE).getValue())
                        .heatIncome(typeToGain.getOrDefault(GainType.HEAT, Gain.NONE).getValue())
                        .plantIncome(typeToGain.getOrDefault(GainType.PLANT, Gain.NONE).getValue())
                        .steelIncome(typeToGain.getOrDefault(GainType.STEEL, Gain.NONE).getValue())
                        .titaniumIncome(typeToGain.getOrDefault(GainType.TITANIUM, Gain.NONE).getValue());
            }
            List<Gain> bonuses = cardMetadata.getBonuses();
            if (!CollectionUtils.isEmpty(bonuses)) {
                Map<GainType, Gain> typeToGain = bonuses.stream().collect(Collectors.toMap(Gain::getType, Function.identity()));
                builder
                        .microbes(typeToGain.getOrDefault(GainType.MICROBE, Gain.NONE).getValue())
                        .animals(typeToGain.getOrDefault(GainType.ANIMAL, Gain.NONE).getValue())
                        .temperature(typeToGain.getOrDefault(GainType.TEMPERATURE, Gain.NONE).getValue())
                        .oxygen(typeToGain.getOrDefault(GainType.OXYGEN, Gain.NONE).getValue())
                        .oceans(typeToGain.getOrDefault(GainType.OCEAN, Gain.NONE).getValue())
                        .forest(typeToGain.getOrDefault(GainType.FOREST, Gain.NONE).getValue())
                        .cards(typeToGain.getOrDefault(GainType.CARD, Gain.NONE).getValue())
                        .terraformingRating(typeToGain.getOrDefault(GainType.TERRAFORMING_RATING, Gain.NONE).getValue());
            }

            if (cardMetadata.getWinPointsInfo() != null) {
                WinPointsInfo winPointsInfo = cardMetadata.getWinPointsInfo();
                if (winPointsInfo.getType() == CardCollectableResource.EARTH) {
                    builder.immigrationShuttles(1);
                } else if (winPointsInfo.getType() == CardCollectableResource.JUPITER) {
                    builder.vpPerJupiter(1);
                }
            }

            if (cardMetadata.getCardAction() != null) {
                switch (cardMetadata.getCardAction()) {
                    case ASTROFARM:
                        builder.microbes(2);
                        break;
                    case HEAT_EARTH_INCOME:
                        builder.heatEarthIncome(1);
                        break;
                    case MC_EARTH_INCOME:
                        builder.mcEarthIncome(1);
                        break;
                    case MC_SCIENCE_INCOME:
                        builder.mcScienceIncome(1);
                        break;
                    case MC_2_BUILDING_INCOME:
                        builder.mcTwoBuildingIncome(1);
                        break;
                    case MC_ENERGY_INCOME:
                        builder.mcEnergyIncome(1);
                        break;
                    case HEAT_SPACE_INCOME:
                        builder.heatSpaceIncome(1);
                        break;
                    case MC_SPACE_INCOME:
                        builder.mcSpaceIncome(1);
                        break;
                    case MC_EVENT_INCOME:
                        builder.mcEventIncome(1);
                        break;
                    case HEAT_ENERGY_INCOME:
                        builder.heatEnergyIncome(1);
                        break;
                    case PLANT_MICROBE_INCOME:
                        builder.plantMicrobeIncome(1);
                        break;
                    case MC_FOREST_INCOME:
                        builder.mcForestIncome(1);
                        break;
                    case MC_ANIMAL_PLANT_INCOME:
                        builder.mcAnimalPlantIncome(1);
                        break;
                    case CARD_SCIENCE_INCOME:
                        builder.cardScienceIncome(1);
                        break;
                    case AUTOMATED_FACTORIES:
                    case TALL_STATION:
                        builder.buildWith9Discount(1);
                        break;
                    case BUILDING_INDUSTRIES:
                        builder.buildingIndustries(1);
                        break;
                    case ENERGY_STORAGE:
                        builder.energyStorage(1);
                        break;
                    case EOS_CHASMA:
                        builder.animals(1);
                        break;
                    case FUEL_FACTORY:
                        builder.fuelFactory(1);
                        break;
                    case PROCESSED_METALS:
                        builder.processedMetals(1);
                        break;
                    case PROCESSING_PLANT:
                        builder.processingPlant(1);
                        break;
                }
            }
        }

        if (!CollectionUtils.isEmpty(card.getOxygenRequirement())) {
            if (card.getOxygenRequirement().get(0)  == ParameterColor.R) {
                builder.redOxygenAndMore(1);
            }
            if (card.getOxygenRequirement().size() == 1 && card.getOxygenRequirement().get(0) == ParameterColor.P) {
                builder.oxygenPurpleRequirement(1);
            }
        }

        if (!CollectionUtils.isEmpty(card.getTemperatureRequirement())) {
            if (card.getTemperatureRequirement().get(0)  == ParameterColor.R) {
                builder.redTemperatureAndMore(1);
            }
            if (card.getTemperatureRequirement().get(0)  == ParameterColor.P && card.getTemperatureRequirement().size() == 2) {
                builder.purpleOrRedTemperature(1);
            }

            if (card.getTemperatureRequirement().get(0)  == ParameterColor.W && card.getTemperatureRequirement().size() == 1) {
                builder.whiteTemperature(1);
            }

            if (card.getTemperatureRequirement().get(0)  == ParameterColor.Y && card.getTemperatureRequirement().size() == 2) {
                builder.yellowWhiteTemperature(1);
            }


        }

        if (card.getOceanRequirement() != null) {
            if (card.getOceanRequirement().getMinValue() > 0) {
                builder.minOceans(card.getOceanRequirement().getMinValue());
            } else if (card.getOceanRequirement().getMinValue() < Constants.MAX_OCEANS) {
                builder.maxOceans(card.getOceanRequirement().getMaxValue());
            }
        }

        if (!CollectionUtils.isEmpty(card.getTagRequirements())) {
            if (card.getTagRequirements().contains(Tag.JUPITER)) {
                builder.beamFromThorium(1);
            }

            if (card.getTagRequirements().contains(Tag.ENERGY) && card.getTagRequirements().size() == 2) {
                builder.fusionPower(1);
            }

            if (card.getTagRequirements().contains(Tag.SCIENCE)) {
                builder.scienceRequirement(card.getTagRequirements().size());
            }
        }

        if (card.getClass() == Microprocessors.class) {
            builder.microprocessors(1);
        }

        builder.winPoints(card.getWinningPoints());

        return builder.build();
    }


    private MarsCardRow getMarsCardRow(MarsGame game, Map<Integer, Integer> allCardToIndex, int cardId, Player currentPlayer, Player anotherPlayer) {
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

        return MarsCardRow.builder().turn(game.getTurns())
                .cards(embeddedDenseVector[allCardToIndex.get(cardId)])
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

                .spaceTags(cardService.countPlayedTags(currentPlayer, Set.of(Tag.SPACE)))
                .earthTags(cardService.countPlayedTags(currentPlayer, Set.of(Tag.EARTH)))
                .eventTags(cardService.countPlayedTags(currentPlayer, Set.of(Tag.EVENT)))
                .scienceTags(cardService.countPlayedTags(currentPlayer, Set.of(Tag.SCIENCE)))
                .plantTags(cardService.countPlayedTags(currentPlayer, Set.of(Tag.PLANT)))
                .energyTags(cardService.countPlayedTags(currentPlayer, Set.of(Tag.ENERGY)))
                .buildingTags(cardService.countPlayedTags(currentPlayer, Set.of(Tag.BUILDING)))
                .animalTags(cardService.countPlayedTags(currentPlayer, Set.of(Tag.ANIMAL)))
                .jupiterTags(cardService.countPlayedTags(currentPlayer, Set.of(Tag.JUPITER)))
                .microbeTags(cardService.countPlayedTags(currentPlayer, Set.of(Tag.MICROBE)))

                .oxygenLevel(game.getPlanet().getOxygenValue())
                .temperatureLevel(game.getPlanet().getTemperatureValue())
                .oceansLevel(Constants.MAX_OCEANS - game.getPlanet().oceansLeft())
                .opponentWinPoints(winPointsService.countWinPointsWithFloats(anotherPlayer, game))
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
                .opponentExtraSeeCards(draftCardsService.countExtraCardsToDraft(currentPlayer))
                .opponentExtraTakeCards(draftCardsService.countExtraCardsToTake(currentPlayer))
                .build();
    }

    private float testState(MarsCardRow marsCardRow, Player player) {
        Tensor someInput = new Tensor(getForUse(marsCardRow));
        someInput.div(player.isFirstBot() ? MAX_INPUTS_TENSOR_1 : MAX_INPUTS_TENSOR_2);

        FeedForwardNetwork feedForwardNetwork = player.isFirstBot() ? networkThreadLocal1.get() : networkThreadLocal2.get();

        feedForwardNetwork.setInput(someInput);

        return feedForwardNetwork.getOutput()[0];
    }

    public float testState(MarsCardRow marsCardRow, int cardId, int network) {
        Map<Integer, Integer> allCardToIndex = cardFactory.getAllCardIdToIndex();

        marsCardRow.setCards(embeddedDenseVector[allCardToIndex.get(cardId)]);

        Tensor someInput = new Tensor(getForUse(marsCardRow));
        someInput.div(network == 1 ? MAX_INPUTS_TENSOR_1 : MAX_INPUTS_TENSOR_2);

        FeedForwardNetwork feedForwardNetwork = network == 1 ? networkThreadLocal1.get() : networkThreadLocal2.get();

        feedForwardNetwork.setInput(someInput);

        return feedForwardNetwork.getOutput()[0];
    }

    public void markWinner(String winner) {
        data.get(winner).forEach(data -> data.setWinner(1));
    }

    public Map<String, List<MarsCardRow>> getData() {
        return data;
    }

    public float[] getForStudy(MarsCardRow row) {
        int blueCardsSize = blueCardIdToIndex.size();


        float[] result = new float[row.cards.length + 58 + blueCardsSize];
        for (int i = 0; i < row.cards.length; i++) {
            result[i] = row.cards[i];
        }
        int counter = row.cards.length;

        result[counter++] = row.turn;

        result[counter++] = row.winPoints;
        result[counter++] = row.mcIncome;
        result[counter++] = row.mc;
        result[counter++] = row.steelIncome;
        result[counter++] = row.titaniumIncome;
        result[counter++] = row.plantsIncome;
        result[counter++] = row.plants;
        result[counter++] = row.heatIncome;
        result[counter++] = row.heat;
        result[counter++] = row.cardsIncome;
        result[counter++] = row.cardsBuilt;
        result[counter++] = row.extraSeeCards;
        result[counter++] = row.extraTakeCards;

        result[counter++] = row.greenCards;
        result[counter++] = row.redCards;
        result[counter++] = row.blueCards;

        result[counter++] = row.heatEarthIncome;
        result[counter++] = row.mcAnimalPlantIncome;
        result[counter++] = row.cardScienceIncome;
        result[counter++] = row.mcEarthIncome;
        result[counter++] = row.plantPlantIncome;
        result[counter++] = row.mcScienceIncome;
        result[counter++] = row.mcTwoBuildingIncome;
        result[counter++] = row.mcEnergyIncome;
        result[counter++] = row.mcSpaceIncome;
        result[counter++] = row.heatSpaceIncome;
        result[counter++] = row.mcEventIncome;
        result[counter++] = row.heatEnergyIncome;
        result[counter++] = row.plantMicrobeIncome;
        result[counter++] = row.mcForestIncome;

        result[counter++] = row.spaceTags;
        result[counter++] = row.earthTags;
        result[counter++] = row.eventTags;
        result[counter++] = row.scienceTags;
        result[counter++] = row.plantTags;
        result[counter++] = row.energyTags;
        result[counter++] = row.buildingTags;
        result[counter++] = row.animalTags;
        result[counter++] = row.jupiterTags;
        result[counter++] = row.microbeTags;

        result[counter++] = row.oxygenLevel;
        result[counter++] = row.temperatureLevel;
        result[counter++] = row.oceansLevel;

        result[counter++] = row.opponentWinPoints;
        result[counter++] = row.opponentMcIncome;
        result[counter++] = row.opponentMc;
        result[counter++] = row.opponentSteelIncome;
        result[counter++] = row.opponentTitaniumIncome;
        result[counter++] = row.opponentPlantsIncome;
        result[counter++] = row.opponentPlants;
        result[counter++] = row.opponentHeatIncome;
        result[counter++] = row.opponentHeat;
        result[counter++] = row.opponentCardsIncome;
        result[counter++] = row.opponentCardsBuilt;
        result[counter++] = row.opponentExtraSeeCards;
        result[counter++] = row.opponentExtraTakeCards;

        for (Integer blueCard : row.getBlueCardsList()) {
            if (!blueCardIdToIndex.containsKey(blueCard)) {
                continue;
            }
            result[counter + blueCardIdToIndex.get(blueCard)] = 1;
        }

        counter += blueCardsSize;

        result[counter++] = row.winner;

        return result;
    }

    public float[] getForUse(MarsCardRow row) {
        int blueCardsSize = blueCardIdToIndex.size();


        float[] result = new float[row.cards.length + 57 + blueCardsSize];
        for (int i = 0; i < row.cards.length; i++) {
            result[i] = row.cards[i];
        }
        int counter = row.cards.length;

        result[counter++] = row.turn;

        result[counter++] = row.winPoints;
        result[counter++] = row.mcIncome;
        result[counter++] = row.mc;
        result[counter++] = row.steelIncome;
        result[counter++] = row.titaniumIncome;
        result[counter++] = row.plantsIncome;
        result[counter++] = row.plants;
        result[counter++] = row.heatIncome;
        result[counter++] = row.heat;
        result[counter++] = row.cardsIncome;
        result[counter++] = row.cardsBuilt;
        result[counter++] = row.extraSeeCards;
        result[counter++] = row.extraTakeCards;

        result[counter++] = row.greenCards;
        result[counter++] = row.redCards;
        result[counter++] = row.blueCards;

        result[counter++] = row.heatEarthIncome;
        result[counter++] = row.mcAnimalPlantIncome;
        result[counter++] = row.cardScienceIncome;
        result[counter++] = row.mcEarthIncome;
        result[counter++] = row.plantPlantIncome;
        result[counter++] = row.mcScienceIncome;
        result[counter++] = row.mcTwoBuildingIncome;
        result[counter++] = row.mcEnergyIncome;
        result[counter++] = row.mcSpaceIncome;
        result[counter++] = row.heatSpaceIncome;
        result[counter++] = row.mcEventIncome;
        result[counter++] = row.heatEnergyIncome;
        result[counter++] = row.plantMicrobeIncome;
        result[counter++] = row.mcForestIncome;

        result[counter++] = row.spaceTags;
        result[counter++] = row.earthTags;
        result[counter++] = row.eventTags;
        result[counter++] = row.scienceTags;
        result[counter++] = row.plantTags;
        result[counter++] = row.energyTags;
        result[counter++] = row.buildingTags;
        result[counter++] = row.animalTags;
        result[counter++] = row.jupiterTags;
        result[counter++] = row.microbeTags;

        result[counter++] = row.oxygenLevel;
        result[counter++] = row.temperatureLevel;
        result[counter++] = row.oceansLevel;

        result[counter++] = row.opponentWinPoints;
        result[counter++] = row.opponentMcIncome;
        result[counter++] = row.opponentMc;
        result[counter++] = row.opponentSteelIncome;
        result[counter++] = row.opponentTitaniumIncome;
        result[counter++] = row.opponentPlantsIncome;
        result[counter++] = row.opponentPlants;
        result[counter++] = row.opponentHeatIncome;
        result[counter++] = row.opponentHeat;
        result[counter++] = row.opponentCardsIncome;
        result[counter++] = row.opponentCardsBuilt;
        result[counter++] = row.opponentExtraSeeCards;
        result[counter++] = row.opponentExtraTakeCards;

        for (Integer blueCard : row.getBlueCardsList()) {
            if (!blueCardIdToIndex.containsKey(blueCard)) {
                continue;
            }
            result[counter + blueCardIdToIndex.get(blueCard)] = 1;
        }

        counter += blueCardsSize;

        return result;
    }
}
