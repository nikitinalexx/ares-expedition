package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.dto.DraftCardsDto;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.dl4j.JudgeOracle;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.network2.dto.CardWithChanceModifier;
import com.terraforming.ares.services.ai.network2.projection.*;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.validation.input.NuclearPlantsOnBuiltEffectValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class Network2PickPhaseService {
    private static final Map<Integer, Double> ALL_PHASES = Map.of(1, 0d, 2, 0d, 3, 0d, 4, 0d, 5, 0d);
    private static final int CARDS_TO_LOOK_AHEAD = 50;
    private static final int PHASE_5_ITERATIONS = 10;

    private final ScenarioEngine scenarioEngine;
    private final BatchProjectionService batchProjectionService;
    private final NNService nnService;
    private final IDataCollect iDataCollect;
    private final CardService cardService;
    private final DraftCardsService draftCardsService;
    private final Network2ProjectBuildService network2ProjectBuildService;
    private final Network2ThirdPhaseActionProjector network2ThirdPhaseActionProjector;
    private final JudgeOracle judgeOracle;
    private final NuclearPlantsOnBuiltEffectValidator nuclearPlantsOnBuiltEffectValidator;

    public int pickPhase(MarsGame game, Player player) {
        if (AiConstants.ENABLE_AI_EXPLORATION) {
            return choosePhaseWithExploration(game, player);
        } else {
            return pickPhaseNoExploration(game, player);
        }
    }

    public int pickPhaseNoExploration(MarsGame game, Player player) {
        Map<Integer, Double> allPhases = new HashMap<>(ALL_PHASES);
        if (player.getPreviousChosenPhase() != null) {
            allPhases.remove(player.getPreviousChosenPhase());
        }

        if (allPhases.containsKey(1)) {
            allPhases.put(1, getFirstPhaseChance(game, player.getUuid()));
        }

        if (allPhases.containsKey(2)) {
            allPhases.put(2, getSecondPhaseChance(game, player.getUuid()));
        }

        if (allPhases.containsKey(3)) {
            allPhases.put(3, getThirdPhaseChance(game, player.getUuid()));
        }

        if (allPhases.containsKey(4)) {
            allPhases.put(4, getFourthPhaseChance(game, player.getUuid()));
        }

        if (allPhases.containsKey(5)) {
            allPhases.put(5, getFifthPhaseChance(game, player.getUuid()));
        }

        if (Constants.LOG_NET_COMPARISON_V2) {
            System.out.println(judgeOracle.predict(iDataCollect.collectData(game, player)));
            allPhases.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()) // Сортируем по номеру фазы для порядка
                    .forEach(entry -> {
                        System.out.printf("Phase %d: [%6.2f%%]%n",
                                entry.getKey(),
                                entry.getValue() * 100);
                    });
        }



        return allPhases.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    public int choosePhaseWithExploration(MarsGame game, Player player) {

        Map<Integer, Double> allPhases = new HashMap<>(ALL_PHASES);

        if (player.getPreviousChosenPhase() != null) {
            allPhases.remove(player.getPreviousChosenPhase());
        }

        if (allPhases.containsKey(1)) {
            allPhases.put(1, getFirstPhaseChance(game, player.getUuid()));
        }
        if (allPhases.containsKey(2)) {
            allPhases.put(2, getSecondPhaseChance(game, player.getUuid()));
        }
        if (allPhases.containsKey(3)) {
            allPhases.put(3, getThirdPhaseChance(game, player.getUuid()));
        }
        if (allPhases.containsKey(4)) {
            allPhases.put(4, getFourthPhaseChance(game, player.getUuid()));
        }
        if (allPhases.containsKey(5)) {
            allPhases.put(5, getFifthPhaseChance(game, player.getUuid()));
        }

        double temperature = phaseExplorationTemperature(game);

        double maxScore = allPhases.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        List<Integer> phases = new ArrayList<>(allPhases.keySet());
        double[] weights = new double[phases.size()];

        double sum = 0.0;
        for (int i = 0; i < phases.size(); i++) {
            double score = allPhases.get(phases.get(i));
            double w = Math.exp((score - maxScore) / temperature);
            weights[i] = w;
            sum += w;
        }

        double r = ThreadLocalRandom.current().nextDouble() * sum;
        double acc = 0.0;

        for (int i = 0; i < phases.size(); i++) {
            acc += weights[i];
            if (r <= acc) {
                return phases.get(i);
            }
        }

        return phases.getLast();
    }

    private double phaseExplorationTemperature(MarsGame game) {

        double expectedGameLength = 28.0;
        double progress = game.getTurns() / expectedGameLength;
        progress = Math.min(progress, 1.0);

        double startTemp = 0.12;
        double endTemp = 0.02;

        return startTemp * (1.0 - progress) + endTemp * progress;
    }

    private Double getThirdPhaseChance(MarsGame game, String uuid) {
        game = new MarsGame(game);
        game.setCurrentPhase(3);

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player player = game.getPlayerByUuid(uuid);
        player.setChosenPhase(3);
        player.setBlueActionExtraActivationsLeft(1);

        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);
        anotherPlayer.setChosenPhase(4);

        if (player.hasPhaseUpgrade(Constants.PHASE_3_UPGRADE_DOUBLE_REPEAT)) {
            player.setBlueActionExtraActivationsLeft(2);
        }

        return network2ThirdPhaseActionProjector.processTurn(game, player, anotherPlayer);
    }

    private Double getFirstPhaseChance(MarsGame game, String playerUuid) {
        game = new MarsGame(game);
        game.setDummyHandMode(false);
        game.getPlayerUuidToPlayer().values().forEach(p -> {
            p.setChosenPhase(1);
        });
        game.setCurrentPhase(1);

        Player player = game.getPlayerByUuid(playerUuid);
        addFirstPhaseBuilds(player);

        return getBestPhaseScenarioFinalChance(game, player);
    }

    private Double getSecondPhaseChance(MarsGame game, String playerUuid) {
        game = new MarsGame(game);
        game.setDummyHandMode(false);
        game.getPlayerUuidToPlayer().values().forEach(p -> {
            p.setChosenPhase(2);
        });
        game.setCurrentPhase(2);

        Player player = game.getPlayerByUuid(playerUuid);
        addSecondPhaseBuilds(player);

        return getBestPhaseScenarioFinalChance(game, player);
    }

    private Double getFourthPhaseChance(MarsGame game, String playerUuid) {
        game = new MarsGame(game);

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player player = game.getPlayerByUuid(playerUuid);
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        addIncome(player);
        addIncome(anotherPlayer);

        if (player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_EXTRA_MC)) {
            player.setMc(player.getMc() + 7);
        } else if (player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_DOUBLE_PRODUCE)) {
            player.setMc(player.getMc() + 1);
        }

        Set<Card> cardsThatCanPayAgain = player.getPlayed().getCards().stream().map(cardService::getCard).filter(card -> card.getColor() == CardColor.GREEN && card.canPayAgain()).collect(Collectors.toSet());

        if (!player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_EXTRA_MC) || cardsThatCanPayAgain.isEmpty()) {
            return nnService.predictBatch(List.of(iDataCollect.collectData(game, player)), player).getFirst().baseProb;
        }

        List<float[]> statesToCheck = new ArrayList<>();
        State stateBeforeDoublePayment = getStateBeforePayment(player);

        for (Card card : cardsThatCanPayAgain) {
            card.payAgain(game, cardService, player);
            int extraCards = player.getHand().size() - stateBeforeDoublePayment.cards;
            if (extraCards > 0) {
                for (int i = 0; i < extraCards; i++) {
                    player.getHand().getCards().removeLast();
                }
                for (int i = 0; i < extraCards; i++) {
                    player.getHand().addCard(AiConstants.GENERIC_DUMMY_ID);
                }
            }
            statesToCheck.add(iDataCollect.collectData(game, player));
            restoreStateBeforePayment(player, stateBeforeDoublePayment);
        }

        return nnService.predictBatch(statesToCheck, player).stream().mapToDouble(p -> p.baseProb).max().orElseThrow();
    }


    private State getStateBeforePayment(Player player) {
        State state = new State();
        state.mc = player.getMc();
        state.heat = player.getHeat();
        state.plants = player.getPlants();
        state.cards = player.getHand().size();
        return state;
    }

    private void restoreStateBeforePayment(Player player, State state) {
        player.setMc(state.mc);
        player.setHeat(state.heat);
        player.setPlants(state.plants);
        if (player.getHand().size() > state.cards) {
            player.getHand().getCards().subList(state.cards, player.getHand().size()).clear();
        }
    }

    private void addCardIncome(Player player) {
        for (int i = 0; i < player.getCardIncome(); i++) {
            player.getHand().addCard(AiConstants.GENERIC_DUMMY_ID);
        }
    }

    private void addIncome(Player player) {
        player.setMc(player.getMc() + player.getMcIncome() + player.getTerraformingRating());
        player.setHeat(player.getHeat() + player.getHeatIncome());
        player.setPlants(player.getPlants() + player.getPlantsIncome());
        addCardIncome(player);
    }

    private Double getBestPhaseScenarioFinalChance(MarsGame game, Player player) {
        List<Scenario> scenarios = scenarioEngine.generateAllScenarios(game, player);
        if (scenarios.isEmpty()) {
            return 0d;
        }

        ProjectionTask<List<Scenario>> scenariosTask = new ScenariosBatchTask(scenarios);
        batchProjectionService.executeAll(player, List.of(scenariosTask));

        Scenario bestScenario = scenarios.stream()
                .max(Comparator.comparingDouble(Scenario::getFinalChance))
                .orElseThrow();

        return bestScenario.getFinalChance();
    }

    private void addFirstPhaseBuilds(Player player) {
        List<BuildDto> builds = new ArrayList<>();

        if (player.hasPhaseUpgrade(Constants.PHASE_1_UPGRADE_DISCOUNT)) {
            builds.add(new BuildDto(BuildType.GREEN, 6));
        } else if (player.hasPhaseUpgrade(Constants.PHASE_1_UPGRADE_BUILD_EXTRA)) {
            builds.add(new BuildDto(BuildType.GREEN, 3));
            builds.add(new BuildDto(BuildType.GREEN, 0, 12));
        } else {
            builds.add(new BuildDto(BuildType.GREEN, 3));
        }
        player.setBuilds(builds);
    }

    private void addSecondPhaseBuilds(Player player) {
        List<BuildDto> builds = new ArrayList<>();
        builds.add(new BuildDto(BuildType.BLUE_RED));

        if (player.getChosenPhase() == 2) {
            if (player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_MC)) {
                builds.add(new BuildDto(BuildType.BLUE_RED_OR_MC));
            } else if (player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_CARD)) {
                player.getHand().addCard(AiConstants.GENERIC_DUMMY_ID);
                builds.add(new BuildDto(BuildType.BLUE_RED));
            } else {
                builds.add(new BuildDto(BuildType.BLUE_RED_OR_CARD));
            }
        }
        player.setBuilds(builds);
    }

    private Double getFifthPhaseChance(MarsGame game, String playerUuid) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        game = new MarsGame(game);
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player player = game.getPlayerByUuid(playerUuid);
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);
        player.setChosenPhase(5);

        // ===== 1. Определяем правила пятой фазы =====
        DraftCardsDto draftCardsDto = draftCardsService.countCardsToTakeAndDraftByChosenPhase(player);

        int cardsSeen = draftCardsDto.getCardsToSee();
        int cardsTaken = draftCardsDto.getCardsToTake();

        List<Integer> originalHand = new ArrayList<>(player.getHand().getCards());
        List<float[]> baseStatesWithoutCardsInHand = new ArrayList<>();
        baseStatesWithoutCardsInHand.add(iDataCollect.collectData(game, player));

        for (Integer cardId : originalHand) {
            player.getHand().removeCard(cardId);
            baseStatesWithoutCardsInHand.add(iDataCollect.collectData(game, player));
            player.getHand().addCard(cardId);
        }
        player.getHand().getCards().clear();
        player.getHand().getCards().addAll(originalHand);

        List<Prediction> basePredictions = nnService.predictBatch(baseStatesWithoutCardsInHand, player);
        double baseProb = basePredictions.removeFirst().baseProb;
        Map<Integer, Double> cardIdToBaseChance = new HashMap<>();
        for (int i = 0; i < originalHand.size(); i++) {
            cardIdToBaseChance.put(originalHand.get(i), basePredictions.get(i).baseProb);
        }

        Deck projectsDeck = cardService.createProjectsDeck(game.getExpansions());
        projectsDeck.removeCards(player.getHand().getCards());
        projectsDeck.removeCards(player.getPlayed().getCards());
        projectsDeck.removeCards(anotherPlayer.getPlayed().getCards());

        List<Card> allCardsToCheck = Stream.concat(player.getHand().getCards().stream().map(cardService::getCard), projectsDeck.dealCards(Math.min(projectsDeck.size(), CARDS_TO_LOOK_AHEAD)).stream().map(cardService::getCard)).toList();
        List<CardWithChanceModifier> allProjections = network2ProjectBuildService.getBestCardProjectionsIgnoreRequirements(game, player, allCardsToCheck);

        List<Double> handDeltas = new ArrayList<>();
        List<Double> deckDeltas = new ArrayList<>();

        for (CardWithChanceModifier projection : allProjections) {
            double modifier = projection.getModifier();
            boolean isHandCard = originalHand.contains(projection.getCard().getId());
            double delta = (projection.getChance() - (isHandCard ? cardIdToBaseChance.get(projection.getCard().getId()) : baseProb)) * modifier;
            if (isHandCard) {
                handDeltas.add(delta);
            } else {
                deckDeltas.add(delta);
            }
        }
        handDeltas.sort(Comparator.naturalOrder());

        double handValue = 0.0;
        for (int i = 0; i < Math.min(cardsTaken, handDeltas.size()); i++) {
            handValue += handDeltas.get(i);
        }

        double totalDeckValue = 0;
        for (int i = 0; i < PHASE_5_ITERATIONS; i++) {
            List<Double> seenCards = randomSubset(
                    deckDeltas,
                    Math.min(cardsSeen, deckDeltas.size()),
                    random
            );

            seenCards.sort(Comparator.reverseOrder());

            double deckValue = 0;
            for (int j = 0; j < cardsTaken; j++) {
                deckValue += seenCards.get(j);
            }
            totalDeckValue += (deckValue / cardsTaken);
        }
        totalDeckValue /= PHASE_5_ITERATIONS;

        double delta = totalDeckValue - handValue;

        return baseProb + delta * player.getAggression();
    }

    public <T> List<T> randomSubset(List<T> list, int count, Random rnd) {
        int size = list.size();
        if (count >= size) return list;

        return rnd.ints(0, size)
                .distinct()
                .limit(count)
                .mapToObj(list::get)
                .collect(Collectors.toList());
    }

    public double getExpectedDeckValue(List<Double> deckDeltas, int cardsSeen, int cardsTaken) {
        int iterations = PHASE_5_ITERATIONS; // 20
        double grandTotal = 0;

        // Создаем буфер один раз, а не в цикле
        double[] buffer = new double[cardsSeen];
        int deckSize = deckDeltas.size();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < iterations; i++) {
            // Заполняем буфер случайными элементами (имитация randomSubset)
            // Если deckSize >> cardsSeen, риск дубликатов мал, можно даже без distinct
            // Но если нужен честный выбор без повторений - используй простой алгоритм выборки
            for (int j = 0; j < cardsSeen; j++) {
                buffer[j] = deckDeltas.get(rnd.nextInt(deckSize));
            }

            // Быстрая сортировка примитивов (намного быстрее стримов)
            Arrays.sort(buffer);

            // Суммируем K лучших (они в конце массива после сортировки)
            for (int j = 0; j < cardsTaken; j++) {
                grandTotal += buffer[cardsSeen - 1 - j];
            }
        }

        return grandTotal / iterations;
    }

}
