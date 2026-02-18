package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.network2.dto.CardWithChanceModifier;
import com.terraforming.ares.services.simulations.CardPickStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class Network2DiscardCardsProcessor {

    private final AdvancedAiDataCollectionService advancedAiDataCollectionService;
    private final NNService nnService;
    private final CardPickStatistics cardPickStatistics;

    public List<Integer> getBestCards(
            MarsGame game,
            Player player,
            List<Integer> cardsToDiscard,
            int cardsToKeepCount
    ) {
        if (AiConstants.ENABLE_AI_EXPLORATION) {
            return getBestCardsWithExploration(game, player, cardsToDiscard, cardsToKeepCount);
        } else {
            return getBestCardsNoExploration(game, player, cardsToDiscard, cardsToKeepCount);
        }
    }

    public List<Integer> getBestCardsNoExploration(
            MarsGame game,
            Player player,
            List<Integer> cardsToDiscard, // Это кандидаты на то, чтобы их оставить/сбросить
            int cardsToKeepCount
    ) {
        if (cardsToDiscard.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Собираем батч состояний: для каждой карты создаем проекцию "Рука БЕЗ этой карты"
        List<float[]> featureBatch = new ArrayList<>();

        for (Integer cardId : cardsToDiscard) {
            // Временно удаляем карту из руки игрока
            player.getHand().removeCard(cardId);

            // Собираем вектор (AdvancedAiDataCollectionService теперь сам увидит обновленную руку)
            featureBatch.add(advancedAiDataCollectionService.collectData(game, player));

            // Возвращаем карту обратно для следующей итерации
            player.getHand().addCard(cardId);
        }

        // 2. Отправляем всё это одним махом в нейронку
        List<Prediction> predictions = nnService.predictBatch(featureBatch, player);

        // 3. Формируем рейтинг.
        // Чем МЕНЬШЕ вероятность без карты, тем БОЛЬШЕ её ценность.
        record CardRating(int cardId, double probWithoutCard) {}
        List<CardRating> ratedCards = new ArrayList<>();

        for (int i = 0; i < cardsToDiscard.size(); i++) {
            ratedCards.add(new CardRating(
                    cardsToDiscard.get(i),
                    predictions.get(i).baseProb
            ));
        }

        // Сортируем по ВОЗРАСТАНИЮ вероятности (в начале те, без кого нам хуже всего)
        ratedCards.sort(Comparator.comparingDouble(CardRating::probWithoutCard));

        // 4. Отбираем лучшие
        List<Integer> result = new ArrayList<>(cardsToKeepCount);
        for (int i = 0; i < Math.min(cardsToKeepCount, ratedCards.size()); i++) {
            CardRating chosen = ratedCards.get(i);
            result.add(chosen.cardId());

            if (AiConstants.COLLECT_CARD_RANK_STATS) {
                cardPickStatistics.onCardPicked(
                        chosen.cardId(),
                        i // Ранг теперь просто индекс в отсортированном списке
                );
            }
        }

        return result;
    }


    public List<Integer> getBestCardsWithExploration(
            MarsGame game,
            Player player,
            List<Integer> cardsToDiscard,
            int cardsToKeepCount
    ) {
        if (cardsToDiscard.isEmpty()) return Collections.emptyList();

        // 1. Собираем батч: "Шанс БЕЗ этой карты"
        List<float[]> featureBatch = new ArrayList<>();
        for (Integer cardId : cardsToDiscard) {
            player.getHand().removeCard(cardId);
            featureBatch.add(advancedAiDataCollectionService.collectData(game, player));
            player.getHand().addCard(cardId);
        }

        List<Prediction> predictions = nnService.predictBatch(featureBatch, player);

        // 2. Формируем рейтинг
        record CardRating(int cardId, double probWithoutCard, int globalRank) {}
        List<CardRating> ratedCards = new ArrayList<>();
        for (int i = 0; i < cardsToDiscard.size(); i++) {
            ratedCards.add(new CardRating(cardsToDiscard.get(i), predictions.get(i).baseProb, -1));
        }

        // Сортируем: в начале самые важные (без кого шанс минимален)
        ratedCards.sort(Comparator.comparingDouble(CardRating::probWithoutCard));

        // Проставляем ранги для статистики
        for (int i = 0; i < ratedCards.size(); i++) {
            CardRating cr = ratedCards.get(i);
            ratedCards.set(i, new CardRating(cr.cardId(), cr.probWithoutCard(), i));
        }

        // 3. Настраиваем окно исследования (Exploration Window)
        // Мы не хотим выбирать совсем мусорные карты, поэтому берем топ-N для выбора
        int windowSize = Math.min(ratedCards.size(), cardsToKeepCount * 2);
        List<CardRating> window = new ArrayList<>(ratedCards.subList(0, windowSize));

        // Температура: чем выше, тем больше рандома
        double temperature = explorationTemperature(game) * 0.01;
        if (temperature < 0.0001) temperature = 0.0001; // Защита от деления на 0

        List<Integer> result = new ArrayList<>(cardsToKeepCount);
        Random rnd = ThreadLocalRandom.current();

        for (int pick = 0; pick < cardsToKeepCount && !window.isEmpty(); pick++) {
            // Чтобы минимизировать probWithoutCard через Softmax,
            // мы инвертируем значения (1.0 - prob) или используем отрицание.
            // Используем max для стабилизации экспоненты (Softmax trick)
            double minProbInWindow = window.stream().mapToDouble(CardRating::probWithoutCard).min().orElse(0);

            double[] weights = new double[window.size()];
            double sum = 0;

            for (int i = 0; i < window.size(); i++) {
                // ВАЖНО: (minProb - currentProb) даст отрицательное число или 0 для лучшей карты.
                // Это заставит лучшую карту иметь наибольший вес.
                double weight = Math.exp((minProbInWindow - window.get(i).probWithoutCard()) / temperature);
                weights[i] = weight;
                sum += weight;
            }

            double r = rnd.nextDouble() * sum;
            double acc = 0;
            int chosenIndex = 0;
            for (int i = 0; i < weights.length; i++) {
                acc += weights[i];
                if (r <= acc) {
                    chosenIndex = i;
                    break;
                }
            }

            CardRating chosen = window.remove(chosenIndex);
            result.add(chosen.cardId());

            if (AiConstants.COLLECT_CARD_RANK_STATS) {
                cardPickStatistics.onCardPicked(chosen.cardId(), chosen.globalRank());
            }
        }

        return result;
    }

    private double explorationTemperature(MarsGame game) {
        int generation = game.getTurns(); // или аналогичный счётчик хода

        double explorationFactor = 0.5;

        if (generation <= 5) {
            return 0.45 * explorationFactor;
        } else if (generation <= 15) {
            return 0.30 * explorationFactor;
        } else if (generation <= 22) {
            return 0.18 * explorationFactor;
        } else {
            return 0.04 * explorationFactor;
        }
    }


}
