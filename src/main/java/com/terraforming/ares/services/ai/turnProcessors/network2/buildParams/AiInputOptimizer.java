package com.terraforming.ares.services.ai.turnProcessors.network2.buildParams;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.turnProcessors.AiUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiInputOptimizer {
    private final AiUtility aiUtility;
    private final NNService nnService;
    private final DataCollectContextProvider dataCollectContextProvider;

    public OptimizedInputDecisions optimizeInputDecisions(
            MarsGame game,
            Player player,
            SharedInputAnalysis analysis
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
                new PhaseUpgradeStep(game, player, phaseUpgradesToSimulate),
                new ResourceStep(game, player, resourcesToSimulate),
                new OxygenStep(game, player, analysis.requiresOxygenCheck),
                new TagWithoutChoiceStep(game, player, needToSimulateTags && !needTagSimulationWithChoice)
        );

        doDecisionSteps(initialSteps, dataCollectContext, player, decisions);


        List<DecisionStep> viralEnhancersStep = List.of(
                new ViralEnhancersChoiceStep(game, player, decisions, analysis.viralEnhancersActive)
        );
        doDecisionSteps(viralEnhancersStep, dataCollectContext, player, decisions);


        List<DecisionStep> tagWithChoiceStep = List.of(
                new TagWithChoiceStep(game, player, anotherPlayer, decisions, analysis, needTagSimulationWithChoice)
        );
        doDecisionSteps(tagWithChoiceStep, dataCollectContext, player, decisions);

        return decisions;
    }

    private void doDecisionSteps(List<DecisionStep> steps, DataCollectContext dataCollectContext, Player player, OptimizedInputDecisions decisions) {
        List<float[]> simulations = new ArrayList<>();

        steps.stream().filter(DecisionStep::isApplicable).forEach(step -> step.collectSimulations(simulations, dataCollectContext));

        List<Prediction> predictions = simulations.isEmpty() ? List.of() : nnService.predictBatch(simulations, player.isFirstBot() ? NNService.ModelType.BASE : NNService.ModelType.OPTIMIZED);
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

        return upgradesToSimulate;
    }


    private Map<InputRequirementType, List<Card>> getInputTypeToCardsToSimulate(Player player, SharedInputAnalysis analysis) {
        Map<InputRequirementType, List<Card>> simulationsToMake = new HashMap<>();

        if (analysis.requiresMicrobeInput) {
            List<Card> microbes = aiUtility.getPlayerCardsWithResource(player, Set.of(CardCollectableResource.MICROBE));
            microbes = microbes.stream().filter(
                    card -> card.getCardMetadata().getCardAction() != CardAction.BACTERIAL_AGGREGATES ||
                            player.getCardResourcesCount().get(BacterialAggregates.class) <= AiConstants.BACTERIAL_AGGREGATES_COUNT_FOR_MICROBE_PUT).collect(Collectors.toList());
            if (!microbes.isEmpty()) {
                simulationsToMake.put(InputRequirementType.MICROBE_INPUT, microbes);
            }
        }

        if (analysis.requiresAnimalInput) {
            List<Card> animals = aiUtility.getPlayerCardsWithResource(player, Set.of(CardCollectableResource.ANIMAL));
            if (!animals.isEmpty()) {
                simulationsToMake.put(InputRequirementType.ANIMAL_INPUT, animals);
            }
        }

        if (analysis.requiresScienceResourceInput) {
            List<Card> scienceCards = aiUtility.getPlayerCardsWithResource(player, Set.of(CardCollectableResource.SCIENCE));
            if (!scienceCards.isEmpty()) {
                simulationsToMake.put(InputRequirementType.SCIENCE_RESOURCE_INPUT, scienceCards);
            }
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
