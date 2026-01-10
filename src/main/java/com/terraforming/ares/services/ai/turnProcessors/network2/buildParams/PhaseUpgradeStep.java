package com.terraforming.ares.services.ai.turnProcessors.network2.buildParams;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.dl4j.Prediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhaseUpgradeStep implements DecisionStep {

    private final MarsGame game;
    private final Player player;
    private final List<Integer> upgrades;

    public PhaseUpgradeStep(
            MarsGame game,
            Player player,
            List<Integer> upgrades
    ) {
        this.game = game;
        this.player = player;
        this.upgrades = upgrades;
    }

    @Override
    public boolean isApplicable() {
        return !upgrades.isEmpty();
    }

    @Override
    public void collectSimulations(List<float[]> simulations, DataCollectContext dataCollectContext) {
        simulations.addAll(
                createPhaseUpgradeSimulations(game, player, upgrades, dataCollectContext)
        );
    }

    @Override
    public void applyPredictions(PredictionCursor cursor, OptimizedInputDecisions decisions) {
        addOptimizedPhaseDecisionsFromPredictions(upgrades, decisions, cursor.next(upgrades.size()));
    }

    private List<float[]> createPhaseUpgradeSimulations(MarsGame game, Player player, List<Integer> upgradesToSimulate, DataCollectContext dataCollectContext) {
        if (upgradesToSimulate.isEmpty()) return List.of();

        List<Integer> initialPhaseCards = new ArrayList<>(player.getPhaseCards());
        List<float[]> result = new ArrayList<>();

        for (Integer phaseUpgrade : upgradesToSimulate) {//(0,1) or (2,3) or (4,5) or (6,7) or (8,9)
            player.getPhaseCards().set(phaseUpgrade / 2, phaseUpgrade % 2 + 1);
            float[] currentFeatures = dataCollectContext.getDataCollect().collectData(game, player);
            result.add(currentFeatures);

            player.setPhaseCards(new ArrayList<>(initialPhaseCards));
        }

        return result;
    }

    private void addOptimizedPhaseDecisionsFromPredictions(
            List<Integer> upgradesToSimulate,
            OptimizedInputDecisions optimizedInputDecisions,
            List<Prediction> predictions
    ) {
        if (upgradesToSimulate.isEmpty() || predictions.isEmpty()) return;

        int bestIndex = -1;
        int secondBestIndex = -1;

        double bestProb = -1.0;
        double secondBestProb = -1.0;

        int bestPhase = -1;

        for (int i = 0; i < predictions.size(); i++) {
            double prob = predictions.get(i).baseProb;
            int upgradeId = upgradesToSimulate.get(i);
            int phase = upgradeId / 2;

            if (prob > bestProb) {
                // сдвигаем лучший в secondBest, если фаза отличается
                if (bestIndex != -1 && phase != bestPhase) {
                    secondBestProb = bestProb;
                    secondBestIndex = bestIndex;
                }

                bestProb = prob;
                bestIndex = i;
                bestPhase = phase;

            } else if (phase != bestPhase && prob > secondBestProb) {
                secondBestProb = prob;
                secondBestIndex = i;
            }
        }

        if (bestIndex != -1) {
            optimizedInputDecisions.setBestPhaseUpgradeOverall(
                    new DecisionWithPrediction<>(upgradesToSimulate.get(bestIndex), predictions.get(bestIndex))
            );
        }

        if (secondBestIndex != -1) {
            optimizedInputDecisions.setSecondBestPhaseUpgradeOverall(
                    new DecisionWithPrediction<>(upgradesToSimulate.get(secondBestIndex), predictions.get(secondBestIndex))
            );
        }

        // Лучшие апгрейды по фазам (как было)
        Map<Integer, Double> bestProbsByPhase = new HashMap<>();

        for (int i = 0; i < upgradesToSimulate.size(); i++) {
            int upgradeId = upgradesToSimulate.get(i);
            double prob = predictions.get(i).baseProb;
            int phase = upgradeId / 2;

            if (prob > bestProbsByPhase.getOrDefault(phase, -1.0)) {
                bestProbsByPhase.put(phase, prob);
                optimizedInputDecisions.getPhaseUpgradeDecisions().put(phase, new DecisionWithPrediction<>(upgradeId, predictions.get(i)));
            }
        }
    }

}

