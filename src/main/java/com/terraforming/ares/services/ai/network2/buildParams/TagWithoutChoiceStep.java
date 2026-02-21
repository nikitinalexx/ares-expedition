package com.terraforming.ares.services.ai.network2.buildParams;

import com.terraforming.ares.cards.corporations.DummyCard;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.turnProcessors.AiUtility;

import java.util.*;

public class TagWithoutChoiceStep implements DecisionStep {

    private final MarsGame game;
    private final Player player;
    private final Player opponent;
    private final boolean requiresTagCheck;
    private final boolean collectByOpponent;

    public TagWithoutChoiceStep(
            MarsGame game,
            Player player,
            Player opponent,
            boolean requiresTagCheck,
            boolean collectByOpponent
    ) {
        this.game = game;
        this.player = player;
        this.opponent = opponent;
        this.requiresTagCheck = requiresTagCheck;
        this.collectByOpponent = collectByOpponent;
    }

    @Override
    public boolean isApplicable() {
        return requiresTagCheck;
    }

    @Override
    public void collectSimulations(List<float[]> simulations, DataCollectContext dataCollectContext) {
        simulations.addAll(createTagSimulationsWithoutChoice(game, player, dataCollectContext));
    }

    @Override
    public void applyPredictions(PredictionCursor cursor, OptimizedInputDecisions decisions) {
        addOptimizedTagDecisions(decisions, cursor.next(Tag.values().length - 1));
    }

    private Collection<float[]> createTagSimulationsWithoutChoice(MarsGame originalGame, Player originalPlayer, DataCollectContext dataCollectContext) {
        CardService cardService = dataCollectContext.getCardService();
        MarsContextProvider marsContextProvider = dataCollectContext.getContextProvider();
        AiUtility aiUtility = dataCollectContext.getAiUtility();
        IDataCollect dataCollect = dataCollectContext.getDataCollect();

        List<float[]> result = new ArrayList<>();

        List<Card> applicableCards = originalPlayer.getPlayed().getCards().stream().map(cardService::getCard).filter(Card::onBuiltEffectApplicableToOther).toList();
        int originalPlayerHandSize = originalPlayer.getHand().getCards().size();

        for (Tag value : Tag.values()) {
            if (value == Tag.DYNAMIC) {
                continue;
            }

            MarsGame gameCopy = new MarsGame(originalGame);
            Player playerCopy = gameCopy.getPlayerByUuid(originalPlayer.getUuid());
            Player opponentCopy = gameCopy.getPlayerByUuid(opponent.getUuid());

            MarsContext context = marsContextProvider.provide(gameCopy, playerCopy);

            Map<Integer, List<Integer>> inputParameters = Map.of(InputFlag.TAG_INPUT.getId(), List.of(value.ordinal()));

            applicableCards.forEach(c -> c.postProjectBuiltEffect(context, new DummyCard(), inputParameters));

            aiUtility.simulateDummyHandInsteadOfNewCards(playerCopy, originalPlayerHandSize);

            float[] currentFeatures = dataCollect.collectData(gameCopy, (collectByOpponent ? opponentCopy : playerCopy).getUuid());
            if (collectByOpponent) {
                dataCollect.modifyOpponentTagCount(currentFeatures, value, 1);
            } else {
                dataCollect.modifyTagCount(currentFeatures, value, 1);
            }
            result.add(currentFeatures);

        }

        return result;
    }

    private void addOptimizedTagDecisions(OptimizedInputDecisions decisions, List<Prediction> tagPredictions) {
        Tag[] allTags = Tag.values();
        List<DecisionWithPrediction<Tag>> allDecisions = new ArrayList<>();

        int predictionIdx = 0;
        for (Tag tag : allTags) {
            if (tag == Tag.DYNAMIC) continue;

            // Просто собираем всё в список
            allDecisions.add(new DecisionWithPrediction<>(tag, tagPredictions.get(predictionIdx++)));
        }

        // Сортируем по убыванию вероятности
        if (collectByOpponent) {
            allDecisions.sort(Comparator.comparingDouble(a -> a.getPrediction().baseProb));
        } else {
            allDecisions.sort((a, b) -> Double.compare(b.getPrediction().baseProb, a.getPrediction().baseProb));
        }

        // Берем топ-3 (с проверкой на случай, если тегов вдруг меньше 3)
        List<DecisionWithPrediction<Tag>> top3 = allDecisions.subList(0, Math.min(3, allDecisions.size()));

        decisions.setBestTagDecision(new ArrayList<>(top3));
    }

}

