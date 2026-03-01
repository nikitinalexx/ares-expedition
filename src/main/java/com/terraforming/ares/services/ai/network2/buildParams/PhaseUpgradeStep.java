package com.terraforming.ares.services.ai.network2.buildParams;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.dl4j.Prediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PhaseUpgradeStep implements DecisionStep {

    private final MarsGame game;
    private final Player player;
    private final Player opponent;
    private final List<Integer> upgrades;
    private final boolean collectByOpponent;

    public PhaseUpgradeStep(
            MarsGame game,
            Player player,
            Player opponent,
            List<Integer> upgrades,
            boolean collectByOpponent
    ) {
        this.game = game;
        this.player = player;
        this.opponent = opponent;
        this.upgrades = upgrades;
        this.collectByOpponent = collectByOpponent;
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
            float[] currentFeatures = dataCollectContext.getDataCollect().collectData(game, (collectByOpponent ? opponent : player).getUuid());
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

        double bestProb = collectByOpponent ? 1.0 : -1.0;
        double secondBestProb = collectByOpponent ? 1.0 : -1.0;

        int bestPhase = -1;

        for (int i = 0; i < predictions.size(); i++) {
            double prob = predictions.get(i).baseProb;
            int upgradeId = upgradesToSimulate.get(i);
            int phase = upgradeId / 2;

            if (collectByOpponent && prob < bestProb || !collectByOpponent && prob > bestProb) {
                // сдвигаем лучший в secondBest, если фаза отличается
                if (bestIndex != -1 && phase != bestPhase) {
                    secondBestProb = bestProb;
                    secondBestIndex = bestIndex;
                }

                bestProb = prob;
                bestIndex = i;
                bestPhase = phase;

            } else if (phase != bestPhase && (collectByOpponent && prob < secondBestProb || !collectByOpponent && prob > secondBestProb)) {
                secondBestProb = prob;
                secondBestIndex = i;
            }
        }

        if (bestIndex != -1) {
            Integer upgradeId = upgradesToSimulate.get(bestIndex);
            int phase = upgradeId / 2;
            int upgradeType = upgradeId % 2;
            if (phase == 3 &&  upgradeType == 0 && (game.getTurns() > 10  && ThreadLocalRandom.current().nextInt(10) < 8 || game.getTurns() <= 10 && ThreadLocalRandom.current().nextInt(10) < 5)) {
                upgradeId++;
            }
            optimizedInputDecisions.setBestPhaseUpgradeOverall(
                    new DecisionWithPrediction<>(upgradeId, predictions.get(bestIndex))
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

            if (collectByOpponent && prob < bestProbsByPhase.getOrDefault(phase, 1.0) || !collectByOpponent && prob > bestProbsByPhase.getOrDefault(phase, -1.0)) {
                bestProbsByPhase.put(phase, prob);
                optimizedInputDecisions.getPhaseUpgradeDecisions().put(phase, new DecisionWithPrediction<>(upgradeId, predictions.get(i)));
            }
        }
    }

}

