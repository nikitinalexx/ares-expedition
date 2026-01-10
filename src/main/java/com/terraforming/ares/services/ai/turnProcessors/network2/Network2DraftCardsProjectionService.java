package com.terraforming.ares.services.ai.turnProcessors.network2;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.turnProcessors.network2.dto.CardWithChanceModifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Network2DraftCardsProjectionService {
    public static final int CARDS_TO_LOOK_AHEAD = 30;
    public static final double MIN_AVERAGE_VALUE_GAP = 0.01;

    private final CardService cardService;
    private final NNService nnService;
    private final AdvancedAiDataCollectionService advancedAiDataCollectionService;
    private final Network2ProjectBuildService network2ProjectBuildService;

    public List<CardWithChanceModifier> cardsToDiscardByProjectedValue(MarsGame marsGame, Player player) {
        List<Player> players = new ArrayList<>(marsGame.getPlayerUuidToPlayer().values());
        Player anotherPlayer = (players.get(0).getUuid().equals(player.getUuid())) ? players.get(1) : players.get(0);

        Deck projectsDeck = cardService.createProjectsDeck(marsGame.getExpansions());
        projectsDeck.removeCards(player.getHand().getCards());
        projectsDeck.removeCards(player.getPlayed().getCards());
        projectsDeck.removeCards(anotherPlayer.getPlayed().getCards());

        double currentProb = nnService.predictBatch(List.of(advancedAiDataCollectionService.collectData(marsGame, player)), NNService.ModelType.OPTIMIZED).getFirst().baseProb;

        List<Card> cardsFromDeck = projectsDeck.dealCards(Math.min(projectsDeck.size(), CARDS_TO_LOOK_AHEAD)).stream().map(cardService::getCard).toList();
        List<CardWithChanceModifier> projections = network2ProjectBuildService.getBestCardProjectionsIgnoreRequirements(marsGame, player, cardsFromDeck);

        // 3. Считаем средний шанс через Delta-Modifier
        double avgChance = projections.stream().mapToDouble(p -> calculateAdjustedChance(p, currentProb)).average().orElse(currentProb);


        return network2ProjectBuildService.getBestCardProjectionsIgnoreRequirements(marsGame, player, player.getHand().getCards().stream().map(cardService::getCard).toList())
                .stream().peek(c -> {
                    c.setChance(calculateAdjustedChance(c, avgChance));
                    c.setModifier(1.0);
                })
                .filter(c -> c.getChance() < avgChance - MIN_AVERAGE_VALUE_GAP)
                .sorted(Comparator.comparingDouble(CardWithChanceModifier::getChance))
                .toList();
    }

    // Вспомогательный метод для расчета по формуле Delta
    private double calculateAdjustedChance(CardWithChanceModifier p, double currentProb) {
        double delta = p.getChance() - currentProb;

        // Применяем модификатор к дельте (неважно, положительная она или отрицательная)
        double adjustedDelta = delta * p.getModifier();

        // Итоговый результат с ограничением от 0 до 1
        double finalChance = currentProb + adjustedDelta;

        return Math.max(0.0, Math.min(1.0, finalChance));
    }

}
