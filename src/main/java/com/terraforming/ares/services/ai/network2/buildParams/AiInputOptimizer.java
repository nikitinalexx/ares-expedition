package com.terraforming.ares.services.ai.network2.buildParams;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiInputOptimizer {
    private final NNService nnService;
    private final DataCollectContextProvider dataCollectContextProvider;
    private final CardService cardService;

    public OptimizedInputDecisions optimizeInputDecisions(
            MarsGame game,
            Player player,
            SharedInputAnalysis analysis
    ) {
        return optimizeInputDecisions(game, player, analysis, false);
    }

    public OptimizedInputDecisions optimizeInputDecisions(
            MarsGame game,
            Player player,
            SharedInputAnalysis analysis,
            boolean collectByOpponent
    ) {
        DataCollectContext dataCollectContext = dataCollectContextProvider.getDataCollectContext();

        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0).getUuid().equals(player.getUuid()) ? players.get(1) : players.get(0);

        OptimizedInputDecisions decisions = new OptimizedInputDecisions();

        // ======== Подготовка данных (как и раньше) ========

        List<Integer> phaseUpgradesToSimulate = getPhaseUpgradesToSimulate(player, analysis);
        Map<InputRequirementType, List<Card>> resourcesToSimulate = getInputTypeToCardsToSimulate(player, analysis);

        boolean needToSimulateTags = analysis.requiresTagChoice;
        boolean needTagSimulationWithChoice = needToSimulateTags && (analysis.decomposersActive || analysis.marsUniversityActive || analysis.viralEnhancersActive);

        // ======== ФАЗА 1: Predictive steps ========

        List<DecisionStep> initialSteps = List.of(
                new PhaseUpgradeStep(game, player, anotherPlayer, phaseUpgradesToSimulate, collectByOpponent),
                new ResourceStep(game, player, anotherPlayer, resourcesToSimulate, collectByOpponent),
                new OxygenStep(game, player, analysis.requiresOxygenCheck),
                new TagWithoutChoiceStep(game, player, anotherPlayer, needToSimulateTags && !needTagSimulationWithChoice, collectByOpponent),
                new SyntheticCatastropheStep(game, player, analysis.redCardTargets)
        );

        doDecisionSteps(initialSteps, dataCollectContext, player, anotherPlayer, decisions, collectByOpponent);


        List<DecisionStep> viralEnhancersStep = List.of(
                new ViralEnhancersChoiceStep(game, player, decisions, analysis.viralEnhancersActive)
        );
        doDecisionSteps(viralEnhancersStep, dataCollectContext, player, anotherPlayer, decisions, collectByOpponent);


        List<DecisionStep> tagWithChoiceStep = List.of(
                new TagWithChoiceStep(game, player, anotherPlayer, decisions, analysis, needTagSimulationWithChoice, collectByOpponent)
        );
        doDecisionSteps(tagWithChoiceStep, dataCollectContext, player, anotherPlayer, decisions, collectByOpponent);

        return decisions;
    }

    private void doDecisionSteps(List<DecisionStep> steps, DataCollectContext dataCollectContext, Player player, Player opponent, OptimizedInputDecisions decisions, boolean collectByOpponent) {
        List<float[]> simulations = new ArrayList<>();

        steps.stream().filter(DecisionStep::isApplicable).forEach(step -> step.collectSimulations(simulations, dataCollectContext));

        List<Prediction> predictions = simulations.isEmpty() ? List.of() : nnService.predictBatch(simulations, collectByOpponent ? opponent : player);
        PredictionCursor cursor = new PredictionCursor(predictions);
        steps.stream().filter(DecisionStep::isApplicable).forEach(step -> step.applyPredictions(cursor, decisions));
    }


    private List<Integer> getPhaseUpgradesToSimulate(Player player, SharedInputAnalysis analysis) {
        if (analysis.requiresPhaseUpgrade) {
            return getAllAvailableUpgrades(player);
        }

        List<Integer> upgradesToSimulate = new ArrayList<>();
        if (analysis.requiresPhase1Upgrade) {
            upgradesToSimulate.addAll(getUpgradesForPhase(player, 1));
        }

        if (analysis.requiresPhase2Upgrade) {
            upgradesToSimulate.addAll(getUpgradesForPhase(player, 2));
        }

        if (analysis.requiresPhase3Upgrade) {
            upgradesToSimulate.addAll(getUpgradesForPhase(player, 3));
        }

        if (analysis.requiresPhase4Upgrade) {
            upgradesToSimulate.addAll(getUpgradesForPhase(player, 4));
        }

        if (analysis.requiresPhase5Upgrade) {
            upgradesToSimulate.addAll(getUpgradesForPhase(player, 5));
        }

        return upgradesToSimulate;
    }


    private Map<InputRequirementType, List<Card>> getInputTypeToCardsToSimulate(Player player, SharedInputAnalysis analysis) {
        Map<InputRequirementType, List<Card>> simulationsToMake = new HashMap<>();

        if (analysis.requiresMicrobeInput && !CollectionUtils.isEmpty(analysis.microbeTargets)) {
            simulationsToMake.put(InputRequirementType.MICROBE_INPUT, analysis.microbeTargets.stream().map(cardService::getCard).collect(Collectors.toList()));
        }

        if (analysis.requiresAnimalInput && !CollectionUtils.isEmpty(analysis.animalTargets)) {
            simulationsToMake.put(InputRequirementType.ANIMAL_INPUT, analysis.animalTargets.stream().map(cardService::getCard).collect(Collectors.toList()));
        }

        if (analysis.requiresScienceResourceInput && !CollectionUtils.isEmpty(analysis.scienceResourceTargets)) {
            simulationsToMake.put(InputRequirementType.SCIENCE_RESOURCE_INPUT, analysis.scienceResourceTargets.stream().map(cardService::getCard).collect(Collectors.toList()));
        }

        return simulationsToMake;
    }


    private List<Integer> getAllAvailableUpgrades(Player player) {
        List<Integer> allAvailableUpgrades = new ArrayList<>();
        for (int i = 0; i < player.getPhaseCards().size(); i++) {
            int phaseUpgrade = player.getPhaseCards().get(i);
            if (phaseUpgrade == 0) {
                allAvailableUpgrades.add(i * 2);
                allAvailableUpgrades.add(i * 2 + 1);
            } else if (phaseUpgrade == 1) {
                allAvailableUpgrades.add(i * 2 + 1);
            } else {
                allAvailableUpgrades.add(i * 2);
            }
        }
        return allAvailableUpgrades;
    }

    private List<Integer> getUpgradesForPhase(Player player, int phase) {
        int idx = phase - 1;
        int val = player.getPhaseCards().get(idx);
        if (val == 0) return List.of(idx * 2, idx * 2 + 1);
        return List.of(val == 1 ? idx * 2 + 1 : idx * 2);
    }

}
