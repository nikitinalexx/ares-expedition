package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.dataset.MarsCardRow;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardFactory;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.WinPointsService;
import deepnetts.net.FeedForwardNetwork;
import deepnetts.util.FileIO;
import deepnetts.util.Tensor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CardsCollectService {
    private static final float[] MAX_INPUTS  = new float[]{0.9981f, 0.9814f, 0.9977f, 0.9874f, 0.9985f, 0.9945f, 0.9984f, 0.9852f, 0.993f, 0.9993f, 0.9963f, 0.9974f, 0.9942f, 0.9997f, 0.9909f, 0.9958f, 0.9971f, 0.9901f, 0.9848f, 0.9927f, 0.9929f, 0.9961f, 0.9884f, 0.9989f, 0.9997f, 0.9975f, 0.9935f, 0.9946f, 0.9966f, 0.9827f, 0.9964f, 0.9866f, 39.0f, 82.0f, 103.0f, 726.0f, 10.0f, 13.0f, 23.0f, 130.0f, 58.0f, 413.0f, 9.0f, 26.0f, 42.0f, 23.0f, 13.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 14.0f, 30.0f, 9.0f, 82.0f, 103.0f, 726.0f, 10.0f, 13.0f, 23.0f, 130.0f, 58.0f, 413.0f, 9.0f, 42.0f};


    private static final Tensor MAX_INPUTS_TENSOR = new Tensor(MAX_INPUTS);

    private final CardFactory cardFactory;
    private final WinPointsService winPointsService;
    private final CardService cardService;

    private final Map<String, List<MarsCardRow>> data = new ConcurrentHashMap<>();

    private final float[][] embeddedDenseVector = new float[151][];

    private ThreadLocal<FeedForwardNetwork> networkThreadLocal;

    public CardsCollectService(CardFactory cardFactory,
                               WinPointsService winPointsService,
                               CardService cardService) throws IOException, ClassNotFoundException {
        this.cardFactory = cardFactory;
        this.winPointsService = winPointsService;
        this.cardService = cardService;

        Random random = new Random(1678834079073L);

        for (int i = 0; i < 151; i++) {
            float[] matrix = new float[32];
            for (int j = 0; j < matrix.length; j++) {
                matrix[j] = random.nextFloat();
            }
            embeddedDenseVector[i] = matrix;
        }

        networkThreadLocal = ThreadLocal.withInitial(
                () -> {
                    try {
                        return FileIO.createFromFile("cards.dnet", FeedForwardNetwork.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public void collectData(MarsGame game, Player currentPlayer, int card, int turn) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        Player anotherPlayer = players.get(0) == currentPlayer
                ? players.get(1)
                : players.get(0);


        Map<Integer, Integer> redGreenCardIdToIndex = cardFactory.getRedGreenCardIdToIndex();

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

        MarsCardRow marsCardRow = MarsCardRow.builder().turn(turn)
                .cards(embeddedDenseVector[redGreenCardIdToIndex.get(card)])
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
                .build();

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
        Map<Integer, Integer> redGreenCardIdToIndex = cardFactory.getRedGreenCardIdToIndex();


        float bestChance = -1;
        Integer bestCardId = null;

        for (Integer cardId : cards) {
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

            MarsCardRow marsCardRow = MarsCardRow.builder().turn(game.getTurns())
                    .cards(embeddedDenseVector[redGreenCardIdToIndex.get(cardId)])
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
                    .build();

            float newState = testState(marsCardRow);
            if (newState > bestChance) {
                bestChance = newState;
                bestCardId = cardId;
            }
        }

        return bestCardId;
    }

    private float testState(MarsCardRow marsCardRow) {
        Tensor someInput = new Tensor(marsCardRow.getAsInput());
        someInput.div(MAX_INPUTS_TENSOR);

        FeedForwardNetwork feedForwardNetwork = networkThreadLocal.get();

        feedForwardNetwork.setInput(someInput);

        return feedForwardNetwork.getOutput()[0];
    }

    public void markWinner(String winner) {
        data.get(winner).forEach(data -> data.setWinner(1));
    }

    public Map<String, List<MarsCardRow>> getData() {
        return data;
    }
}
