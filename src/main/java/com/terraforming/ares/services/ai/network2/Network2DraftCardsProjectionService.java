package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.network2.dto.CardWithChanceModifier;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.services.policyai.PolicyCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Network2DraftCardsProjectionService {
    public static final int CARDS_TO_LOOK_AHEAD = 30;
    public static final double RELATIVE_GAP_FACTOR = 0.02;
    public static final double MIN_ABSOLUTE_GAP = 0.003;

    private final CardService cardService;
    private final NNService nnService;
    private final AdvancedAiDataCollectionService iDataCollect;
    private final PolicyCollectService policyCollectService;


    public List<CardWithChanceModifier> cardsToDiscardByProjectedValue(MarsGame marsGame, Player player) {
        List<Integer> handCards = new ArrayList<>(player.getHand().getCards());
        if (handCards.isEmpty()) return List.of();

        List<Player> players = new ArrayList<>(marsGame.getPlayerUuidToPlayer().values());
        Player anotherPlayer = (players.get(0).getUuid().equals(player.getUuid())) ? players.get(1) : players.get(0);

        // 1. Текущая база (шанс со всей рукой)
        float[] currentFeatures = iDataCollect.collectData(marsGame, player.getUuid());
        double currentProb = nnService.predictBatch(List.of(currentFeatures), player).getFirst().baseProb;

        // 2. Оцениваем ценность владения каждой картой (Ownership Value)
        // Убираем по одной и смотрим, насколько просядет шанс
        List<float[]> statesWithoutOne = new ArrayList<>();
        for (Integer cardId : handCards) {
            player.getHand().removeCard(cardId);
            statesWithoutOne.add(iDataCollect.collectData(marsGame, player.getUuid()));
            player.getHand().addCard(cardId);
        }

        // 3. Оцениваем потенциал колоды (Deck Potential)
        // Нам нужно знать, на что мы в среднем можем рассчитывать при доборе
        Deck deck = cardService.createProjectsDeck(marsGame.getExpansions());
        deck.removeCards(player.getHand().getCards());
        deck.removeCards(player.getPlayed().getCards());
        deck.removeCards(anotherPlayer.getPlayed().getCards());

        List<Integer> deckSample = deck.dealCards(Math.min(deck.size(), CARDS_TO_LOOK_AHEAD));

        List<float[]> statesWithExtra = new ArrayList<>();
        for (Integer deckCardId : deckSample) {
            player.getHand().addCard(deckCardId);
            statesWithExtra.add(iDataCollect.collectData(marsGame, player.getUuid()));
            player.getHand().removeCard(deckCardId);
        }

        // 1. Собираем всё в один список
        List<float[]> totalBatch = new ArrayList<>(statesWithoutOne);
        totalBatch.addAll(statesWithExtra);

        // 2. Делаем ОДИН вызов нейронки
        List<Prediction> allPreds = nnService.predictBatch(totalBatch, player);

        // 3. Распределяем результаты обратно
        // Первые N предсказаний — это шансы без одной карты
        List<Prediction> preds = allPreds.subList(0, statesWithoutOne.size());

        // Остальные — это шансы с доп. картой из колоды
        double avgDeckProb = allPreds.subList(statesWithoutOne.size(), allPreds.size()).stream()
                .mapToDouble(p -> p.baseProb)
                .average()
                .orElse(currentProb);

        // 4. Динамический порог (Gap)
        // Чем ближе мы к победе, тем меньше должны рисковать, скидывая карты
        double dynamicGap = Math.max(MIN_ABSOLUTE_GAP, (1.0 - currentProb) * RELATIVE_GAP_FACTOR);

        // 5. Формируем результат
        List<CardWithChanceModifier> results = new ArrayList<>();
        for (int i = 0; i < handCards.size(); i++) {
            int cardId = handCards.get(i);
            double probWithoutThisCard = preds.get(i).baseProb;

            // Карта считается "кандидатом на выброс", если:
            // Вероятность БЕЗ неё не сильно ниже, чем средняя вероятность с новой картой из колоды минус зазор.
            // Или проще: если владение этой картой дает профит меньше, чем ожидание от колоды.
            if (probWithoutThisCard > avgDeckProb - dynamicGap) {
                CardWithChanceModifier cm = new CardWithChanceModifier(cardService.getCard(cardId), probWithoutThisCard, 1.0f);
                results.add(cm);
            }
        }

        // Сортируем: в начале самые "безопасные" для удаления карты (те, без которых шанс выше)
        results.sort(Comparator.comparingDouble(CardWithChanceModifier::getChance).reversed());

        return results;
    }

    private final SpecialEffectsService specialEffectsService;
    private final AiTurnService aiTurnService;

    public void performProactiveSale(MarsGame game, Player player) {
        List<Integer> hand = new ArrayList<>(player.getHand().getCards());
        if (hand.isEmpty()) return;

        int cardPrice = specialEffectsService.getCardPrice(player);

        // 3. Оцениваем каждую карту на "токсичность"
        List<float[]> statesWithCardSold = new ArrayList<>();
        statesWithCardSold.add(iDataCollect.collectData(game, player.getUuid()));

        for (Integer cardId : hand) {
            player.getHand().removeCard(cardId);
            player.setMc(player.getMc() + cardPrice);

            statesWithCardSold.add(iDataCollect.collectData(game, player.getUuid()));
            player.setMc(player.getMc() - cardPrice); // Откат
            player.getHand().addCard(cardId);
        }

        List<Prediction> preds = nnService.predictBatch(statesWithCardSold, player);
        double baseProb = preds.removeFirst().baseProb;

        for (int i = 0; i < hand.size(); i++) {
            int cardId = hand.get(i);
            double probWithoutCard = preds.get(i).baseProb;

            double relativeImprovement = (probWithoutCard - baseProb) / (1.0001 - baseProb);
            if (relativeImprovement > 0.01) { // Улучшение шансов на 1% от оставшегося пути к победе
                policyCollectService.sellCards(game, player, List.of(cardService.getCard(cardId)));
                aiTurnService.sellCards(player, game, List.of(cardId));
                return;
            }
        }
    }

}
