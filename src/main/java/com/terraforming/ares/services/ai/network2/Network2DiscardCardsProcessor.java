package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.network2.dto.CardWithChanceModifier;
import com.terraforming.ares.services.simulations.CardPickStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class Network2DiscardCardsProcessor {

    private final CardService cardService;
    private final AdvancedAiDataCollectionService advancedAiDataCollectionService;
    private final NNService nnService;
    private final Network2ProjectBuildService network2ProjectBuildService;
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
            List<Integer> cardsToDiscard,
            int cardsToKeepCount
    ) {
        if (cardsToDiscard.isEmpty()) {
            return Collections.emptyList();
        }

        double currentProb = nnService
                .predictBatch(
                        List.of(advancedAiDataCollectionService.collectData(game, player)),
                        player
                )
                .getFirst()
                .baseProb;

        List<CardWithChanceModifier> projections =
                network2ProjectBuildService.getBestCardProjectionsIgnoreRequirements(
                        game,
                        player,
                        cardsToDiscard.stream().map(cardService::getCard).toList()
                );

        record CardRating(int cardId, double expectedProb, int globalRank) {
        }

        List<CardRating> ratedCards = new ArrayList<>(projections.size());

        for (int i = 0; i < projections.size(); i++) {
            CardWithChanceModifier p = projections.get(i);

            double futureProb = p.getChance();
            double mod = p.getModifier();
            double delta = futureProb - currentProb;
            double finalScore = currentProb + (delta * mod);

            ratedCards.add(new CardRating(cardsToDiscard.get(i), finalScore, -1));
        }

        // Жёсткая сортировка — убеждение модели
        ratedCards.sort(Comparator.comparingDouble(CardRating::expectedProb).reversed());

        // Проставляем глобальный ранг
        for (int i = 0; i < ratedCards.size(); i++) {
            CardRating cr = ratedCards.get(i);
            ratedCards.set(i, new CardRating(cr.cardId(), cr.expectedProb(), i));
        }

        // Берём строго top-N
        List<Integer> result = new ArrayList<>(cardsToKeepCount);

        for (int i = 0; i < Math.min(cardsToKeepCount, ratedCards.size()); i++) {
            CardRating chosen = ratedCards.get(i);

            result.add(chosen.cardId());

            if (AiConstants.COLLECT_CARD_RANK_STATS) {
                cardPickStatistics.onCardPicked(
                        chosen.cardId(),
                        chosen.globalRank()
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
        if (cardsToDiscard.isEmpty()) {
            return Collections.emptyList();
        }

        double currentProb = nnService
                .predictBatch(
                        List.of(advancedAiDataCollectionService.collectData(game, player)),
                        player
                )
                .getFirst()
                .baseProb;

        List<CardWithChanceModifier> projections =
                network2ProjectBuildService.getBestCardProjectionsIgnoreRequirements(
                        game,
                        player,
                        cardsToDiscard.stream().map(cardService::getCard).toList()
                );

        record CardRating(int cardId, double expectedProb, int globalRank) {
        }

        List<CardRating> ratedCards = new ArrayList<>(projections.size());

        for (int i = 0; i < projections.size(); i++) {
            CardWithChanceModifier p = projections.get(i);

            double futureProb = p.getChance();
            double mod = p.getModifier();
            double delta = futureProb - currentProb;
            double finalScore = currentProb + (delta * mod);

            ratedCards.add(new CardRating(cardsToDiscard.get(i), finalScore, -1));
        }

        ratedCards.sort(Comparator.comparingDouble(CardRating::expectedProb).reversed());

        for (int i = 0; i < ratedCards.size(); i++) {
            CardRating cr = ratedCards.get(i);
            ratedCards.set(i, new CardRating(cr.cardId(), cr.expectedProb(), i));
        }

        int windowMultiplier = 2;
        int windowSize = Math.min(
                ratedCards.size(),
                cardsToKeepCount * windowMultiplier
        );

        List<CardRating> window = new ArrayList<>(ratedCards.subList(0, windowSize));

        double temperature = explorationTemperature(game) * 0.01;

        List<Integer> result = new ArrayList<>(cardsToKeepCount);
        Random rnd = ThreadLocalRandom.current();

        for (int pick = 0; pick < cardsToKeepCount && !window.isEmpty(); pick++) {

            double maxScore = window.stream()
                    .mapToDouble(CardRating::expectedProb)
                    .max()
                    .orElse(0);

            double[] weights = new double[window.size()];
            double sum = 0;

            for (int i = 0; i < window.size(); i++) {
                double w = Math.exp((window.get(i).expectedProb() - maxScore) / temperature);
                weights[i] = w;
                sum += w;
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
                cardPickStatistics.onCardPicked(
                        chosen.cardId(),
                        chosen.globalRank()
                );
            }
        }

        return result;
    }

    private double explorationTemperature(MarsGame game) {
        int generation = game.getTurns(); // или аналогичный счётчик хода

        double explorationFactor = 1;

        if (generation <= 5) {
            return 0.45 * explorationFactor;
        } else if (generation <= 15) {
            return 0.30 * explorationFactor;
        } else if (generation <= 22) {
            return 0.18 * explorationFactor;
        } else {
            return 0.08 * explorationFactor;
        }
    }


//    public List<Integer> getBestCards(MarsGame game, Player player, List<Integer> cardsToDiscard, int cardsToKeepCount) {
//        if (cardsToDiscard.isEmpty()) return Collections.emptyList();
//
//        double currentProb = nnService.predictBatch(List.of(advancedAiDataCollectionService.collectData(game, player)), NNService.ModelType.OPTIMIZED).getFirst().baseProb;
//
//        List<CardWithChanceModifier> bestCardProjections = network2ProjectBuildService.getBestCardProjectionsIgnoreRequirements(game, player, cardsToDiscard.stream().map(cardService::getCard).toList());
//
//        // 3. Рейтингуем через дельту
//        record CardRating(int cardId, double expectedProb) {
//        }
//        List<CardRating> ratedCards = new ArrayList<>();
//
//        for (int i = 0; i < bestCardProjections.size(); i++) {
//            CardWithChanceModifier cardWithChanceModifier = bestCardProjections.get(i);
//            double futureProb = cardWithChanceModifier.getChance();
//            double mod = cardWithChanceModifier.getModifier();
//            double delta = futureProb - currentProb;
//
//            double finalScore = currentProb + (delta * mod);
//
//            ratedCards.add(new CardRating(cardsToDiscard.get(i), finalScore));
//        }
//
//        // 4. Сортируем: сверху всегда будет тот, у кого expectedProb выше
//        // (даже если они все ниже currentProb)
//        return ratedCards.stream()
//                .sorted(Comparator.comparingDouble(CardRating::expectedProb).reversed())
//                .limit(cardsToKeepCount)
//                .map(CardRating::cardId)
//                .collect(Collectors.toList());
//    }

}
