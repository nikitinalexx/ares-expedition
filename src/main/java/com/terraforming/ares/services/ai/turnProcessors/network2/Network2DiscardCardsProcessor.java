package com.terraforming.ares.services.ai.turnProcessors.network2;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.turnProcessors.network2.dto.CardWithChanceModifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Network2DiscardCardsProcessor {

    private final CardService cardService;
    private final AdvancedAiDataCollectionService advancedAiDataCollectionService;
    private final NNService nnService;
    private final Network2ProjectBuildService network2ProjectBuildService;

    public List<Integer> getBestCards(MarsGame game, Player player, List<Integer> cardsToDiscard, int cardsToKeepCount) {
        if (cardsToDiscard.isEmpty()) return Collections.emptyList();

        NNService.ModelType modelType = player.isFirstBot() ? NNService.ModelType.BASE : NNService.ModelType.OPTIMIZED;

        double currentProb = nnService.predictBatch(List.of(advancedAiDataCollectionService.collectData(game, player)), modelType).getFirst().baseProb;

        List<CardWithChanceModifier> bestCardProjections = network2ProjectBuildService.getBestCardProjectionsIgnoreRequirements(game, player, cardsToDiscard.stream().map(cardService::getCard).toList());

        // 3. Рейтингуем через дельту
        record CardRating(int cardId, double expectedProb) {
        }
        List<CardRating> ratedCards = new ArrayList<>();

        for (int i = 0; i < bestCardProjections.size(); i++) {
            CardWithChanceModifier cardWithChanceModifier = bestCardProjections.get(i);
            double futureProb = cardWithChanceModifier.getChance();
            double mod = cardWithChanceModifier.getModifier();
            double delta = futureProb - currentProb;

            double finalScore = currentProb + (delta * mod);

            ratedCards.add(new CardRating(cardsToDiscard.get(i), finalScore));
        }

        // 4. Сортируем: сверху всегда будет тот, у кого expectedProb выше
        // (даже если они все ниже currentProb)
        return ratedCards.stream()
                .sorted(Comparator.comparingDouble(CardRating::expectedProb).reversed())
                .limit(cardsToKeepCount)
                .map(CardRating::cardId)
                .collect(Collectors.toList());
    }

}
