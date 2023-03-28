package com.terraforming.ares.dataset;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.blue.AdaptationTechnology;
import com.terraforming.ares.cards.blue.AdvancedAlloys;
import com.terraforming.ares.cards.blue.Birds;
import com.terraforming.ares.cards.blue.Tardigrades;
import com.terraforming.ares.cards.green.Microprocessors;
import com.terraforming.ares.cards.red.*;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.winPoints.WinPointsInfo;
import com.terraforming.ares.services.*;
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
    private static final float[] MAX_INPUTS_1 = new float[]{37.0f, 40.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 4.0f, 2.0f, 5.0f, 3.0f, 2.0f, 2.0f, 3.0f, 1.0f, 3.0f, 1.0f, 2.0f, 2.0f, 2.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 4.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 6.0f, 3.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 48.0f, 104.0f, 106.0f, 335.0f, 12.0f, 13.0f, 30.0f, 71.0f, 62.0f, 207.0f, 11.0f, 59.0f, 5.0f, 4.0f, 16.0f, 9.0f, 11.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 23.0f, 15.0f, 19.0f, 21.0f, 13.0f, 14.0f, 28.0f, 7.0f, 10.0f, 12.0f, 14.0f, 30.0f, 9.0f, 115.25f, 106.0f, 335.0f, 12.0f, 13.0f, 30.0f, 83.0f, 65.0f, 205.0f, 11.0f, 60.0f, 5.0f, 4.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};

    private static final float[] MAX_INPUTS_2 = new float[]{37.0f, 40.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 4.0f, 2.0f, 5.0f, 3.0f, 2.0f, 2.0f, 3.0f, 1.0f, 3.0f, 1.0f, 2.0f, 2.0f, 2.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 4.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 6.0f, 3.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 48.0f, 104.0f, 106.0f, 335.0f, 12.0f, 13.0f, 30.0f, 71.0f, 62.0f, 207.0f, 11.0f, 59.0f, 5.0f, 4.0f, 16.0f, 9.0f, 11.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 23.0f, 15.0f, 19.0f, 21.0f, 13.0f, 14.0f, 28.0f, 7.0f, 10.0f, 12.0f, 14.0f, 30.0f, 9.0f, 115.25f, 106.0f, 335.0f, 12.0f, 13.0f, 30.0f, 83.0f, 65.0f, 205.0f, 11.0f, 60.0f, 5.0f, 4.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};


    private static final Tensor MAX_INPUTS_TENSOR_1 = new Tensor(MAX_INPUTS_1);
    private static final Tensor MAX_INPUTS_TENSOR_2 = new Tensor(MAX_INPUTS_2);


    private final CardFactory cardFactory;
    private final WinPointsService winPointsService;
    private final CardService cardService;
    private final DeepNetwork deepNetwork;

    private final DiscountService discountService;

    private final DraftCardsService draftCardsService;
    private final Map<String, List<MarsCardRow>> data = new ConcurrentHashMap<>();

    private ThreadLocal<FeedForwardNetwork> networkThreadLocal1;
    private ThreadLocal<FeedForwardNetwork> networkThreadLocal2;


    private final Map<Integer, Integer> blueCardIdToIndex = new HashMap<>();

    public CardsAiService(CardFactory cardFactory,
                          WinPointsService winPointsService,
                          CardService cardService,
                          DraftCardsService draftCardsService,
                          DeepNetwork deepNetwork,
                          DiscountService discountService) throws IOException, ClassNotFoundException {
        this.cardFactory = cardFactory;
        this.winPointsService = winPointsService;
        this.cardService = cardService;
        this.draftCardsService = draftCardsService;
        this.deepNetwork = deepNetwork;
        this.discountService = discountService;

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

        MarsCardRow marsCardRow = getMarsCardRow(game, card, currentPlayer, anotherPlayer);

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

        float bestChance = -1;
        Integer bestCardId = null;

        for (Integer cardId : cards) {
            MarsCardRow marsCardRow = getMarsCardRow(game, cardId, currentPlayer, anotherPlayer);

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

        float worstChance = (canBeNull ? deepNetwork.testState(game, game.getPlayerByUuid(playerUuid)) : Float.MAX_VALUE);
        Integer worstCardId = null;

        for (Integer cardId : cards) {
            MarsCardRow marsCardRow = getMarsCardRow(game, cardId, currentPlayer, anotherPlayer);

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

        float worstChance = startChance;
        Integer worstCardId = null;

        for (Integer cardId : cards) {
            MarsCardRow marsCardRow = getMarsCardRow(game, cardId, currentPlayer, anotherPlayer);

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

        return testState(getMarsCardRow(game, card, currentPlayer, anotherPlayer), currentPlayer);
    }

    private MarsCardAiData createCardAiData(Card card, int discount) {
        CardMetadata cardMetadata = card.getCardMetadata();

        MarsCardAiData.MarsCardAiDataBuilder builder = MarsCardAiData.builder()
                .price(card.getPrice())
                .discount(discount)
                .spaceTags(card.getTags().contains(Tag.SPACE) ? 1 : 0)
                .earthTags(card.getTags().contains(Tag.EARTH) ? 1 : 0)
                .eventTags(card.getTags().contains(Tag.EVENT) ? 1 : 0)
                .scienceTags(card.getTags().contains(Tag.SCIENCE) ? 1 : 0)
                .plantTags(card.getTags().contains(Tag.PLANT) ? 1 : 0)
                .energyTags(card.getTags().contains(Tag.ENERGY) ? 1 : 0)
                .buildingTags(card.getTags().contains(Tag.BUILDING) ? 1 : 0)
                .animalTags(card.getTags().contains(Tag.ANIMAL) ? 1 : 0)
                .jupiterTags(card.getTags().contains(Tag.JUPITER) ? 1 : 0)
                .microbeTags(card.getTags().contains(Tag.MICROBE) ? 1 : 0);

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
                    case IMPORTED_HYDROGEN:
                        builder.importedHydrogen(1);
                        break;
                    case IMPORTED_NITROGEN:
                        builder.importedNitrogen(1);
                        break;
                    case LARGE_CONVOY:
                        builder.largeConvoy(1);
                        break;
                    case LOCAL_HEAT_TRAPPING:
                        builder.localHeatTrapping(1);
                        break;
                    case NITROGEN_RICH_ASTEROID:
                        builder.nitrogenReach(1);
                        break;
                    case SPECIAL_DESIGN:
                        builder.specialDesign(1);
                        break;
                    case TERRAFORMING_GANYMEDE:
                        builder.terraformingRating(1);
                        break;
                    case WORK_CREWS:
                        builder.workCrews(1);
                        break;
                    case SYNTHETIC_CATASTROPHE:
                        builder.syntheticCatastrophe(1);
                        break;
                    case SCREENING_TECHNOLOGY:
                        builder.advancedScreening(1);
                        break;
                    case AI_CENTRAL:
                        builder.takeCardAction(2);
                        break;
                    case ANAEROBIC_MICROORGANISMS:
                        builder.anaerobicMicro(1);
                        break;
                    case ANTI_GRAVITY_TECH:
                        builder.antiGravityTechnology(1);
                        break;
                    case AQUIFER_PUMPING:
                        builder.aquiferPumping(1);
                        break;
                    case ARCTIC_ALGAE:
                        builder.arcticAlgae(1);
                        break;
                    case ARTIFICIAL_JUNGLE:
                        builder.artificialJungle(1);
                        break;
                    case ASSEMBLY_LINES:
                        builder.assemblyLines(1);
                        break;
                    case ASSET_LIQUIDATION:
                        builder.assetLiquidation(1);
                        break;
                    case BRAINSTORMING_SESSION:
                        builder.brainstormingSession(1);
                        break;
                    case CARETAKER_CONTRACT:
                        builder.caretakerContract(1);
                        break;
                    case CIRCUIT_BOARD:
                        builder.takeCardAction(1);
                        break;
                    case COMMUNITY_GARDENS:
                        builder.communityGardens(1);
                        break;
                    case COMPOSTING_FACTORY:
                        builder.compostingFactory(1);
                        break;
                    case CONSERVED_BIOME:
                        builder.conservedBiome(1);
                        break;
                    case DECOMPOSERS:
                        builder.decomposers(1);
                        break;
                    case DECOMPOSING_FUNGUS:
                        builder.decomposingFungus(1);
                        break;
                    case DEVELOPED_INFRASTRUCTURE:
                        builder.developedInfrastructure(1);
                        break;
                    case DEVELOPMENT_CENTER:
                        builder.developmentCenter(1);
                        break;
                    case EARTH_CATAPULT:
                        builder.globalDiscount(2);
                        break;
                    case ECOLOGICAL_ZONE:
                        builder.ecologicalZone(1);
                        break;
                    case ENERGY_SUBSIDIES:
                        builder.energySubsidies(1);
                        break;
                    case EXTENDED_RESOURCES:
                        builder.keepCards(1);
                        break;
                    case EXTREME_COLD_FUNGUS:
                        builder.extremeColdFungus(1);
                        break;
                    case FARMERS_MARKET:
                        builder.farmersMarket(1);
                        break;
                    case FARMING_COOPS:
                        builder.farmingCoops(1);
                        break;
                    case FISH:
                        builder.fish(1);
                        break;
                    case GHG_PRODUCTION:
                        builder.ghgProduction(1);
                        break;
                    case GREEN_HOUSES:
                        builder.greenHouses(1);
                        break;
                    case HERBIVORES:
                        builder.herbivores(1);
                        break;
                    case HYDRO_ELECTRIC:
                        builder.hydroElectricEnergy(1);
                        break;
                    case INTERPLANETARY_RELATIONS:
                        builder.interplanetaryRelations(1);
                        builder.seeCards(1);
                        builder.keepCards(1);
                        break;
                    case INTERNS:
                        builder.seeCards(2);
                        break;
                    case INTERPLANETARY_CONFERENCE:
                        builder.interplanetaryConference(1);
                        break;
                    case IRON_WORKS:
                        builder.ironworks(1);
                        break;
                    case LIVESTOCK:
                        builder.livestock(1);
                        break;
                    case MARS_UNIVERSITY:
                        builder.marsUniversity(1);
                        break;
                    case MATTER_MANUFACTORING:
                        builder.matterManufactoring(1);
                        break;
                    case MEDIA_GROUP:
                        builder.eventDiscount(5);
                        break;
                    case NITRITE_REDUCTING:
                        builder.nitriteReductingBacteria(1);
                        break;
                    case OLYMPUS_CONFERENCE:
                        builder.olympusConference(1);
                        break;
                    case OPTIMAL_AEROBRAKING:
                        builder.optimalAerobraking(1);
                        break;
                    case PHYSICS_COMPLEX:
                        builder.physicsComplex(1);
                        break;
                    case POWER_INFRASTRUCTURE:
                        builder.powerInfrastructure(1);
                        break;
                    case RECYCLED_DETRITUS:
                        builder.recycledDetritus(1);
                        break;
                    case REDRAFTED_CONTRACTS:
                        builder.redraftedContracts(1);
                        break;
                    case REGOLITH_EATERS:
                        builder.regolithEaters(1);
                        break;
                    case RESEARCH_OUTPOST:
                        builder.globalDiscount(1);
                        break;
                    case SMALL_ANIMALS:
                        builder.smallAnimals(1);
                        break;
                    case RESTRUCTURED_RESOURCES:
                        builder.restructuredResources(1);
                        break;
                    case SOLAR_PUNK:
                        builder.solarpunk(1);
                        break;
                    case STANDARD_TECHNOLOGY:
                        builder.standardTechnology(1);
                        break;
                    case STEELWORKS:
                        builder.steelworks(1);
                        break;
                    case SYMBIOTIC_FUNGUD:
                        builder.symbioticFungus(1);
                        break;
                    case THINKTANK:
                        builder.thinkTank(1);
                        break;
                    case UNITED_PLANETARY:
                        builder.seeCards(1);
                        builder.keepCards(1);
                        break;
                    case VIRAL_ENHANCERS:
                        builder.viralEnhancers(1);
                        break;
                    case VOLCANIC_POOLS:
                        builder.volcanicPools(1);
                        break;
                    case WATER_IMPORT:
                        builder.waterImportFromEuropa(1);
                        builder.vpPerJupiter(1);
                        break;
                    case WOOD_BURNING_STOVES:
                        builder.woodBurningStoves(1);
                        break;
                    case MATTER_GENERATOR:
                        builder.matterGenerator(1);
                        break;
                    case PROGRESSIVE_POLICIES:
                        builder.progressivePolicies(1);
                        break;
                    case FILTER_FEEDERS:
                        builder.filterFeeders(1);
                        break;
                    case SELF_REPLICATING_BACTERIA:
                        builder.selfReplicating(1);
                        break;
                }
            }
        }

        if (!CollectionUtils.isEmpty(card.getOxygenRequirement())) {
            if (card.getOxygenRequirement().get(0) == ParameterColor.R) {
                builder.redOxygenAndMore(1);
            }
            if (card.getOxygenRequirement().size() == 1 && card.getOxygenRequirement().get(0) == ParameterColor.P) {
                builder.oxygenPurpleRequirement(1);
            }

            if (card.getOxygenRequirement().size() == 2 && card.getOxygenRequirement().get(0) == ParameterColor.P) {
                builder.purpleRedOxygen(1);
            }

            if (card.getOxygenRequirement().get(0) == ParameterColor.Y && card.getOxygenRequirement().size() == 2) {
                builder.yellowWhiteOxygen(1);
            }
        }

        if (!CollectionUtils.isEmpty(card.getTemperatureRequirement())) {
            if (card.getTemperatureRequirement().get(0) == ParameterColor.R) {
                builder.redTemperatureAndMore(1);
            }
            if (card.getTemperatureRequirement().get(0) == ParameterColor.P && card.getTemperatureRequirement().size() == 2) {
                builder.purpleOrRedTemperature(1);
            }

            if (card.getTemperatureRequirement().get(0) == ParameterColor.W && card.getTemperatureRequirement().size() == 1) {
                builder.whiteTemperature(1);
            }

            if (card.getTemperatureRequirement().get(0) == ParameterColor.Y && card.getTemperatureRequirement().size() == 2) {
                builder.yellowWhiteTemperature(1);
            }

            if (card.getTemperatureRequirement().get(0) == ParameterColor.P && card.getTemperatureRequirement().size() == 1) {
                builder.purpleTemperature(1);
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
            final List<Tag> tagRequirements = card.getTagRequirements();
            if (tagRequirements.contains(Tag.JUPITER)) {
                builder.beamFromThorium(1);
            }

            if (tagRequirements.contains(Tag.ENERGY) && tagRequirements.size() == 2) {
                builder.fusionPower(1);
            }

            if (tagRequirements.contains(Tag.SCIENCE)) {
                builder.scienceRequirement(tagRequirements.size());
            }
            if (tagRequirements.contains(Tag.ANIMAL) && tagRequirements.size() == 3) {
                builder.advancedEcosystems(1);
            }

            if (tagRequirements.contains(Tag.EVENT) && tagRequirements.size() == 3) {
                builder.crater(1);
            }
        }

        final Class<? extends Card> cl = card.getClass();
        if (cl == Microprocessors.class) {
            builder.microprocessors(1);
        } else if (cl == BusinessContracts.class) {
            builder.businessContracts(1);
        } else if (cl == CeosFavoriteProject.class) {
            builder.ceosFavoriteProject(1);
        } else if (cl == InventionContest.class) {
            builder.inventionContest(1);
        } else if (cl == AssortedEnterprises.class) {
            builder.assortedEnterprises(1);
        } else if (cl == AdaptationTechnology.class) {
            builder.adaptationTechnology(1);
        } else if (cl == AdvancedAlloys.class) {
            builder.advancedAlloys(1);
        } else if (cl == Birds.class) {
            builder.birds(1);
        } else if (cl == Tardigrades.class) {
            builder.tardigrades(1);
        } else if (cl == Research.class) {
            builder.scienceTags(2);
        }

        builder.winPoints(card.getWinningPoints());

        return builder.build();
    }


    private MarsCardRow getMarsCardRow(MarsGame game, int cardId, Player currentPlayer, Player anotherPlayer) {
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

        int discount = discountService.getDiscountForAiStudy(game, cardService.getCard(cardId), currentPlayer);


        return MarsCardRow.builder().turn(game.getTurns())
                .cardData(createCardAiData(cardService.getCard(cardId), discount))
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

    public float testState(MarsCardRow marsCardRow, int cardId, int network, int discount) {
        marsCardRow.setCardData(createCardAiData(cardService.getCard(cardId), discount));

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

        final MarsCardAiData marsCardAiData = row.getCardData();

        float[] result = new float[MarsCardAiData.LENGTH + 58 + blueCardsSize];

        fillAiData(result, marsCardAiData);

        int counter = MarsCardAiData.LENGTH;

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

    private void fillAiData(float[] result, MarsCardAiData marsCardAiData) {
        int counter = 0;

        result[counter++] = marsCardAiData.price;
        result[counter++] = marsCardAiData.discount;
        result[counter++] = marsCardAiData.spaceTags;
        result[counter++] = marsCardAiData.earthTags;
        result[counter++] = marsCardAiData.eventTags;
        result[counter++] = marsCardAiData.scienceTags;
        result[counter++] = marsCardAiData.plantTags;
        result[counter++] = marsCardAiData.energyTags;
        result[counter++] = marsCardAiData.buildingTags;
        result[counter++] = marsCardAiData.animalTags;
        result[counter++] = marsCardAiData.jupiterTags;
        result[counter++] = marsCardAiData.microbeTags;

        result[counter++] = marsCardAiData.mcIncome;
        result[counter++] = marsCardAiData.cardIncome;
        result[counter++] = marsCardAiData.heatIncome;
        result[counter++] = marsCardAiData.plantIncome;
        result[counter++] = marsCardAiData.steelIncome;
        result[counter++] = marsCardAiData.titaniumIncome;

        result[counter++] = marsCardAiData.microbes;
        result[counter++] = marsCardAiData.animals;
        result[counter++] = marsCardAiData.temperature;
        result[counter++] = marsCardAiData.oxygen;
        result[counter++] = marsCardAiData.oceans;
        result[counter++] = marsCardAiData.forest;
        result[counter++] = marsCardAiData.terraformingRating;
        result[counter++] = marsCardAiData.cards;

        result[counter++] = marsCardAiData.heatEarthIncome;
        result[counter++] = marsCardAiData.mcAnimalPlantIncome;
        result[counter++] = marsCardAiData.cardScienceIncome;
        result[counter++] = marsCardAiData.mcEarthIncome;
        result[counter++] = marsCardAiData.plantPlantIncome;
        result[counter++] = marsCardAiData.mcScienceIncome;
        result[counter++] = marsCardAiData.mcTwoBuildingIncome;
        result[counter++] = marsCardAiData.mcEnergyIncome;
        result[counter++] = marsCardAiData.mcSpaceIncome;
        result[counter++] = marsCardAiData.heatSpaceIncome;
        result[counter++] = marsCardAiData.mcEventIncome;
        result[counter++] = marsCardAiData.heatEnergyIncome;
        result[counter++] = marsCardAiData.plantMicrobeIncome;
        result[counter++] = marsCardAiData.mcForestIncome;

        result[counter++] = marsCardAiData.winPoints;
        result[counter++] = marsCardAiData.vpPerJupiter;

        result[counter++] = marsCardAiData.redOxygenAndMore;
        result[counter++] = marsCardAiData.oxygenPurpleRequirement;
        result[counter++] = marsCardAiData.yellowWhiteOxygen;
        result[counter++] = marsCardAiData.purpleRedOxygen;

        result[counter++] = marsCardAiData.redTemperatureAndMore;
        result[counter++] = marsCardAiData.purpleOrRedTemperature;
        result[counter++] = marsCardAiData.whiteTemperature;
        result[counter++] = marsCardAiData.yellowWhiteTemperature;
        result[counter++] = marsCardAiData.purpleTemperature;

        result[counter++] = marsCardAiData.minOceans;
        result[counter++] = marsCardAiData.maxOceans;

        result[counter++] = marsCardAiData.buildWith9Discount;
        result[counter++] = marsCardAiData.beamFromThorium;
        result[counter++] = marsCardAiData.buildingIndustries;
        result[counter++] = marsCardAiData.energyStorage;
        result[counter++] = marsCardAiData.fuelFactory;
        result[counter++] = marsCardAiData.fusionPower;
        result[counter++] = marsCardAiData.immigrationShuttles;
        result[counter++] = marsCardAiData.scienceRequirement;
        result[counter++] = marsCardAiData.microprocessors;
        result[counter++] = marsCardAiData.processedMetals;
        result[counter++] = marsCardAiData.processingPlant;
        result[counter++] = marsCardAiData.advancedEcosystems;
        result[counter++] = marsCardAiData.businessContracts;
        result[counter++] = marsCardAiData.ceosFavoriteProject;
        result[counter++] = marsCardAiData.crater;
        result[counter++] = marsCardAiData.importedHydrogen;
        result[counter++] = marsCardAiData.importedNitrogen;
        result[counter++] = marsCardAiData.inventionContest;
        result[counter++] = marsCardAiData.largeConvoy;
        result[counter++] = marsCardAiData.localHeatTrapping;
        result[counter++] = marsCardAiData.nitrogenReach;
        result[counter++] = marsCardAiData.specialDesign;
        result[counter++] = marsCardAiData.terraformingGanymede;
        result[counter++] = marsCardAiData.workCrews;
        result[counter++] = marsCardAiData.assortedEnterprises;
        result[counter++] = marsCardAiData.syntheticCatastrophe;

        result[counter++] = marsCardAiData.adaptationTechnology;
        result[counter++] = marsCardAiData.advancedAlloys;
        result[counter++] = marsCardAiData.advancedScreening;
        result[counter++] = marsCardAiData.anaerobicMicro;
        result[counter++] = marsCardAiData.antiGravityTechnology;
        result[counter++] = marsCardAiData.aquiferPumping;
        result[counter++] = marsCardAiData.arcticAlgae;
        result[counter++] = marsCardAiData.artificialJungle;
        result[counter++] = marsCardAiData.assemblyLines;
        result[counter++] = marsCardAiData.assetLiquidation;
        result[counter++] = marsCardAiData.birds;
        result[counter++] = marsCardAiData.brainstormingSession;
        result[counter++] = marsCardAiData.caretakerContract;
        result[counter++] = marsCardAiData.takeCardAction;
        result[counter++] = marsCardAiData.communityGardens;
        result[counter++] = marsCardAiData.compostingFactory;
        result[counter++] = marsCardAiData.conservedBiome;
        result[counter++] = marsCardAiData.decomposers;
        result[counter++] = marsCardAiData.decomposingFungus;
        result[counter++] = marsCardAiData.developedInfrastructure;
        result[counter++] = marsCardAiData.developmentCenter;
        result[counter++] = marsCardAiData.globalDiscount;
        result[counter++] = marsCardAiData.ecologicalZone;
        result[counter++] = marsCardAiData.energySubsidies;
        result[counter++] = marsCardAiData.keepCards;
        result[counter++] = marsCardAiData.seeCards;
        result[counter++] = marsCardAiData.extremeColdFungus;
        result[counter++] = marsCardAiData.farmersMarket;
        result[counter++] = marsCardAiData.farmingCoops;
        result[counter++] = marsCardAiData.fish;
        result[counter++] = marsCardAiData.ghgProduction;
        result[counter++] = marsCardAiData.greenHouses;
        result[counter++] = marsCardAiData.herbivores;
        result[counter++] = marsCardAiData.hydroElectricEnergy;
        result[counter++] = marsCardAiData.interplanetaryRelations;
        result[counter++] = marsCardAiData.interplanetaryConference;
        result[counter++] = marsCardAiData.ironworks;
        result[counter++] = marsCardAiData.livestock;
        result[counter++] = marsCardAiData.marsUniversity;
        result[counter++] = marsCardAiData.matterManufactoring;
        result[counter++] = marsCardAiData.eventDiscount;
        result[counter++] = marsCardAiData.nitriteReductingBacteria;
        result[counter++] = marsCardAiData.olympusConference;
        result[counter++] = marsCardAiData.optimalAerobraking;
        result[counter++] = marsCardAiData.physicsComplex;
        result[counter++] = marsCardAiData.powerInfrastructure;
        result[counter++] = marsCardAiData.recycledDetritus;
        result[counter++] = marsCardAiData.redraftedContracts;
        result[counter++] = marsCardAiData.regolithEaters;
        result[counter++] = marsCardAiData.smallAnimals;
        result[counter++] = marsCardAiData.restructuredResources;
        result[counter++] = marsCardAiData.solarpunk;
        result[counter++] = marsCardAiData.standardTechnology;
        result[counter++] = marsCardAiData.steelworks;
        result[counter++] = marsCardAiData.symbioticFungus;
        result[counter++] = marsCardAiData.tardigrades;
        result[counter++] = marsCardAiData.thinkTank;
        result[counter++] = marsCardAiData.viralEnhancers;
        result[counter++] = marsCardAiData.volcanicPools;
        result[counter++] = marsCardAiData.waterImportFromEuropa;
        result[counter++] = marsCardAiData.woodBurningStoves;
        result[counter++] = marsCardAiData.matterGenerator;
        result[counter++] = marsCardAiData.progressivePolicies;
        result[counter++] = marsCardAiData.filterFeeders;
        result[counter++] = marsCardAiData.selfReplicating;
    }

    public float[] getForUse(MarsCardRow row) {
        int blueCardsSize = blueCardIdToIndex.size();

        final MarsCardAiData marsCardAiData = row.getCardData();

        float[] result = new float[MarsCardAiData.LENGTH + 57 + blueCardsSize];

        fillAiData(result, marsCardAiData);

        int counter = MarsCardAiData.LENGTH;

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
