package com.terraforming.ares.services.ai.turnProcessors.network2.buildParams;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.dl4j.Prediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceStep implements DecisionStep {
    public static final List<InputRequirementType> RESOURCE_TRAVERSAL_ORDER = List.of(InputRequirementType.MICROBE_INPUT, InputRequirementType.ANIMAL_INPUT, InputRequirementType.SCIENCE_RESOURCE_INPUT);

    private final MarsGame game;
    private final Player player;
    private final Map<InputRequirementType, List<Card>> resourcesToSimulate;

    public ResourceStep(
            MarsGame game,
            Player player,
            Map<InputRequirementType, List<Card>> resourcesToSimulate
    ) {
        this.game = game;
        this.player = player;
        this.resourcesToSimulate = resourcesToSimulate;
    }

    @Override
    public boolean isApplicable() {
        return !resourcesToSimulate.isEmpty();
    }

    @Override
    public void collectSimulations(List<float[]> simulations, DataCollectContext dataCollectContext) {
        simulations.addAll(createResourceAddSimulations(game, player, dataCollectContext));
    }

    @Override
    public void applyPredictions(PredictionCursor cursor, OptimizedInputDecisions decisions) {
        int resourceCount = resourcesToSimulate.values().stream().mapToInt(List::size).sum();
        addOptimizedResourceDecisions(resourcesToSimulate, decisions, cursor.next(resourceCount));
    }

    private List<float[]> createResourceAddSimulations(MarsGame game, Player player, DataCollectContext dataCollectContext) {
        if (resourcesToSimulate.isEmpty()) return List.of();

        List<float[]> result = new ArrayList<>();

        Map<Class<?>, Integer> initialResourcesCount = new HashMap<>(player.getCardResourcesCount());
        for (InputRequirementType inputType : RESOURCE_TRAVERSAL_ORDER) {
            if (!resourcesToSimulate.containsKey(inputType)) {
                continue;
            }
            List<Card> cardsWithResources = resourcesToSimulate.get(inputType);
            for (Card cardWithResource : cardsWithResources) {
                player.getCardResourcesCount().merge(cardWithResource.getClass(), 1, Integer::sum);
                float[] currentFeatures = dataCollectContext.getDataCollect().collectData(game, player);
                result.add(currentFeatures);
                player.setCardResourcesCount(new HashMap<>(initialResourcesCount));
            }
        }
        return result;
    }

    private void addOptimizedResourceDecisions(Map<InputRequirementType, List<Card>> resourcesToSimulate, OptimizedInputDecisions decisions, List<Prediction> resourcePredictions) {
        Map<InputRequirementType, DecisionWithPrediction<Card>> bestResourceCardPerType = decisions.getBestResourceCardPerType();

        int predictionIdx = 0;
        for (InputRequirementType inputType : RESOURCE_TRAVERSAL_ORDER) {
            if (!resourcesToSimulate.containsKey(inputType)) continue;

            List<Card> cards = resourcesToSimulate.get(inputType);
            Card bestCardForThisType = null;
            Prediction bestPredictionForThisType = null;
            double maxProb = -1.0;

            for (Card card : cards) {
                Prediction prediction = resourcePredictions.get(predictionIdx++);
                double prob = prediction.baseProb;
                if (prob > maxProb) {
                    maxProb = prob;
                    bestCardForThisType = card;
                    bestPredictionForThisType = prediction;
                }
            }

            if (bestCardForThisType != null) {
                bestResourceCardPerType.put(inputType, new DecisionWithPrediction<>(bestCardForThisType, bestPredictionForThisType));
            }
        }

        DecisionWithPrediction<Card> bestMicrobePrediction = bestResourceCardPerType.getOrDefault(InputRequirementType.MICROBE_INPUT, null);
        DecisionWithPrediction<Card> bestAnimalPrediction = bestResourceCardPerType.getOrDefault(InputRequirementType.ANIMAL_INPUT, null);

        Card bestMicrobe = (bestMicrobePrediction != null) ? bestMicrobePrediction.value : null;
        Card bestAnimal = (bestAnimalPrediction != null) ? bestAnimalPrediction.value : null;

        decisions.setAtLeastMicrobeOrAnimalPresent(bestMicrobe != null || bestAnimal != null);

        boolean microbeIsBetter = false;
        if (bestMicrobe != null) {
            if (bestAnimal == null) {
                microbeIsBetter = true;
            } else {
                microbeIsBetter = bestMicrobePrediction.prediction.baseProb > bestAnimalPrediction.prediction.baseProb;
            }
        }
        decisions.setMicrobeIsBetter(microbeIsBetter);
    }

}

