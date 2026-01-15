package com.terraforming.ares.services.ai.network2.buildParams;

import lombok.Data;

import java.util.Set;

@Data
public class SharedInputAnalysis {
    boolean requiresPhaseUpgrade;
    boolean requiresPhase1Upgrade;
    boolean requiresPhase2Upgrade;
    boolean requiresPhase3Upgrade;
    boolean requiresPhase4Upgrade;

    boolean requiresTagChoice;

    boolean requiresMicrobeInput;
    Set<Integer> microbeTargets;
    boolean requiresAnimalInput;
    Set<Integer> animalTargets;
    boolean requiresScienceResourceInput;
    Set<Integer> scienceResourceTargets;

    boolean decomposersActive;
    boolean marsUniversityActive;
    boolean viralEnhancersActive;

    boolean requiresOxygenCheck;
    boolean requiresRedCard;
    Set<Integer> redCardTargets;

    int phaseUpgradeCounter;
}
