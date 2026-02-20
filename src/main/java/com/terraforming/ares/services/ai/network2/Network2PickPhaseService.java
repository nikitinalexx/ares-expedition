package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.dto.DraftCardsDto;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.advanced.features.hand.AllHandCardsFeature;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.network2.projection.*;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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
    private final SpecialEffectsService specialEffectsService;

    public int pickPhase(MarsGame game, Player player) {
        return pickPhaseNoExploration(game, player);
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
            allPhases.put(5, getFifthPhaseChanceAnother(game, player.getUuid()));
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
        state.mc = (short) player.getMc();
        state.heat = (short) player.getHeat();
        state.plants = (short) player.getPlants();
        state.cards = (short) player.getHand().size();
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
        player.setMc(player.getMc() + (player.getMcIncome() + player.getTerraformingRating()));
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
        anotherPlayer.setChosenPhase(4);

        // Фиксируем выбор фаз для корректного сбора данных
        player.setChosenPhase(5);
        // Для оппонента ставим любую другую фазу для баланса
        game.getPlayerUuidToPlayer().values().stream()
                .filter(p -> !p.getUuid().equals(playerUuid))
                .findFirst()
                .ifPresent(p -> p.setChosenPhase(4));

        // 1. Собираем базовую вероятность (Текущее состояние)
        float[] currentFeatures = iDataCollect.collectData(game, player);
        double baseProb = nnService.predictBatch(List.of(currentFeatures), player).getFirst().baseProb;

        // 2. Оцениваем "качество колоды" (Average Value of a New Card)
        // Мы смотрим, насколько одна любая карта из колоды в среднем повышает шанс, попадая в руку
        Deck projectsDeck = cardService.createProjectsDeck(game.getExpansions());
        projectsDeck.removeCards(player.getHand().getCards());
        projectsDeck.removeCards(player.getPlayed().getCards());
        projectsDeck.removeCards(anotherPlayer.getPlayed().getCards());

        // Берем выборку из колоды для оценки
        List<Integer> deckSample = projectsDeck.dealCards(Math.min(projectsDeck.size(), CARDS_TO_LOOK_AHEAD));
        List<float[]> statesWithOneExtra = new ArrayList<>();

        for (Integer cardId : deckSample) {
            player.getHand().addCard(cardId);
            statesWithOneExtra.add(iDataCollect.collectData(game, player));
            player.getHand().removeCard(cardId);
        }

        List<Prediction> samplePreds = nnService.predictBatch(statesWithOneExtra, player);
        List<Double> deckDeltas = samplePreds.stream()
                .map(p -> Math.max(0, p.baseProb - baseProb)) // Нас интересуют только полезные карты
                .collect(Collectors.toList());

        // 3. Симулируем процесс Research (Draft)
        // В фазе 5 мы видим N карт и выбираем M лучших.
        DraftCardsDto draftDto = draftCardsService.countCardsToTakeAndDraftByChosenPhase(player);
        int cardsSeen = draftDto.getCardsToSee();
        int cardsToKeep = draftDto.getCardsToTake();

        double totalDraftBenefit = 0;
        for (int i = 0; i < PHASE_5_ITERATIONS; i++) {
            List<Double> iterationSeen = randomSubset(deckDeltas, Math.min(cardsSeen, deckDeltas.size()), random);
            iterationSeen.sort(Comparator.reverseOrder()); // Выбираем лучшие из увиденных

            double bestCardsValue = 0;
            for (int j = 0; j < Math.min(cardsToKeep, iterationSeen.size()); j++) {
                bestCardsValue += iterationSeen.get(j);
            }
            totalDraftBenefit += bestCardsValue;
        }
        double avgResearchGain = totalDraftBenefit / PHASE_5_ITERATIONS;

        // 4. Оцениваем Mass Effect (прирост ресурсов и само событие фазы)
        // Добавляем "пустышки", чтобы нейронка увидела факт наличия новых карт и денег в руке
        addDummyCards(draftCardsService.countCardsToTakeAndDraftByChosenPhase(anotherPlayer), anotherPlayer);

        float[] finalFeatures = iDataCollect.collectData(game, player);
        double massEffectProb = nnService.predictBatch(List.of(finalFeatures), player).getFirst().baseProb;

        // Итоговая дельта = (Выгода от качества выбранных карт) + (Эффект от самого факта добора/бонусов фазы)
        // Мы используем разницу между massEffectProb и baseProb, чтобы учесть "физический" добор.
        double finalDelta = avgResearchGain + (massEffectProb - baseProb);

        return baseProb + finalDelta;
    }

    private Double getFifthPhaseChanceAnother(MarsGame game, String playerUuid) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        game = new MarsGame(game);
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player player = game.getPlayerByUuid(playerUuid);
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);
        player.setChosenPhase(5);
        anotherPlayer.setChosenPhase(4);

        addDummyCards(draftCardsService.countCardsToTakeAndDraftByChosenPhase(anotherPlayer), anotherPlayer);

        // 1. Базовая вероятность (текущий шанс на победу)
        float[] currentFeatures = iDataCollect.collectData(game, player);
        double baseProb = nnService.predictBatch(List.of(currentFeatures), player).getFirst().baseProb;

        // 2. Подготовка колоды (исключаем то, что уже вышло)
        Deck projectsDeck = cardService.createProjectsDeck(game.getExpansions());
        projectsDeck.removeCards(player.getHand().getCards());
        projectsDeck.removeCards(player.getPlayed().getCards());
        projectsDeck.removeCards(anotherPlayer.getPlayed().getCards());

        // 3. Параметры драфта
        DraftCardsDto draftDto = draftCardsService.countCardsToTakeAndDraftByChosenPhase(player);
        int cardsToSee = draftDto.getCardsToSee();
        int cardsToKeep = draftDto.getCardsToTake();

        // 4. Симуляция синергии через пакетную оценку
        double totalSynergyBenefit = 0;
        int iterations = 50; // Оптимально для точности/скорости

        // 1. ПРЕ-СКОРИНГ: Оцениваем "полезность" каждой карты в вакууме
        // Берем выборку из колоды (например, 20-30 карт), чтобы понять их средний вес
        List<Integer> sampleCards = projectsDeck.dealCards(Math.min(projectsDeck.size(), CARDS_TO_LOOK_AHEAD));
        List<float[]> preScoreStates = new ArrayList<>();
        float[] baseVector = baseVectorWithSomeExtraCards(1, game, player);

        for (Integer cardId : sampleCards) {
            float[] state = baseVector.clone();

            int bitIndex =  AllHandCardsFeature.INDEX_BY_CLASS.get(cardService.getCard(cardId).getClass());
            state[AiConstants.HAND_BIT_MASKS_OFFSET + bitIndex] = 1f;

            preScoreStates.add(state);
        }

        // Получаем веса каждой карты
        List<Prediction> preScores = nnService.predictBatch(preScoreStates, player);
        Map<Integer, Double> cardWeights = new HashMap<>();
        for (int i = 0; i < sampleCards.size(); i++) {
            cardWeights.put(sampleCards.get(i), preScores.get(i).baseProb - baseProb);
        }

        // 2. СИМУЛЯЦИЯ ДРАФТА: Выбираем лучшие из увиденных
        List<float[]> synergyStates = new ArrayList<>();

        float[] baseVectorForSynergy = baseVectorWithSomeExtraCards(cardsToKeep, game, player);

        for (int i = 0; i < iterations; i++) {
            // Имитируем, что мы увидели cardsToSee карт
            List<Integer> seenCards = randomSubset(sampleCards, cardsToSee, random);

            // Сортируем их по нашему пре-скорингу и берем ТОП (имитируем выбор игрока)
            List<Integer> bestToKeep = seenCards.stream()
                    .sorted((id1, id2) -> Double.compare(cardWeights.get(id2), cardWeights.get(id1)))
                    .limit(cardsToKeep)
                    .toList();

            float[] state = baseVectorForSynergy.clone();

            for (int j = 0; j < cardsToKeep; j++) {
                int cardId = bestToKeep.get(j);
                int bitIndex =  AllHandCardsFeature.INDEX_BY_CLASS.get(cardService.getCard(cardId).getClass());
                state[AiConstants.HAND_BIT_MASKS_OFFSET + bitIndex] = 1f;
            }
            synergyStates.add(state);
        }

        // 3. ФИНАЛЬНЫЙ ЗАМЕР: Оцениваем группы с учетом синергии
        List<Prediction> finalPredictions = nnService.predictBatch(synergyStates, player);
        for (Prediction p : finalPredictions) {
            totalSynergyBenefit += (p.baseProb - baseProb);
        }


        // Средний профит от новых комбинаций в руке
        double avgComboGain = totalSynergyBenefit / iterations;

        // 5. Учет стоимости покупки карт (Mass Effect)
        // В 5-й фазе мы не просто получаем карты, мы за них ПЛАТИМ (обычно 3 монеты за штуку)
        // А также получаем бонус фазы (+2 карты или скидки)


        float[] physicsFeatures = iDataCollect.collectData(game, player);
        double physicsProb = nnService.predictBatch(List.of(physicsFeatures), player).getFirst().baseProb;

        // Итоговое предсказание:
        // Шанс с учетом "физики" (трата денег, смена фазы) + Ожидаемая мощь новых комбинаций
        double finalProb = physicsProb + avgComboGain;

        return Math.min(1.0, Math.max(0.0, finalProb));
    }

    private float[] baseVectorWithSomeExtraCards(int cardsToAdd, MarsGame game, Player player) {

// добавляем maxCardsAdded dummy
        for (int i = 0; i < cardsToAdd; i++) {
            player.getHand().addCard(AiConstants.GENERIC_DUMMY_ID);
        }

// получаем базовый вектор
        float[] baseVector = iDataCollect.collectData(game, player);

// удаляем dummy обратно
        for (int i = 0; i < cardsToAdd; i++) {
            player.getHand().removeCard(AiConstants.GENERIC_DUMMY_ID);
        }

        return baseVector;
    }

    private void addDummyCards(DraftCardsDto draftCardsDto, Player player) {
        for (int i = 0; i < draftCardsDto.getCardsToTake(); i++) {
            player.getHand().addCard(AiConstants.GENERIC_DUMMY_ID);
        }
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
