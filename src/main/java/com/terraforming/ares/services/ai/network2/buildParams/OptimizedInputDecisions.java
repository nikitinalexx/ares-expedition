package com.terraforming.ares.services.ai.network2.buildParams;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class OptimizedInputDecisions {
    private DecisionWithPrediction<Integer> bestPhaseUpgradeOverall;
    private DecisionWithPrediction<Integer> secondBestPhaseUpgradeOverall;
    // Для каждой фазы ID лучшего апгрейда
    private Map<Integer, DecisionWithPrediction<Integer>> phaseUpgradeDecisions = new HashMap<>();

    private Map<InputRequirementType, DecisionWithPrediction<Card>> bestResourceCardPerType = new HashMap<>();

    private List<DecisionWithPrediction<Tag>> bestTagDecision;


    private Prediction oxygenPrediction;

    private boolean atLeastMicrobeOrAnimalPresent;
    private boolean microbeIsBetter;
    private boolean viralEnhancersTakePlants;

}
