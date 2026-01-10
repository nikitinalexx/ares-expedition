package com.terraforming.ares.services.ai.turnProcessors.network2.buildParams;

import lombok.Data;

@Data
public class SharedInputAnalysis {
    boolean requiresPhaseUpgrade;
    boolean requiresPhase1Upgrade;
    boolean requiresPhase2Upgrade;
    boolean requiresPhase3Upgrade;
    boolean requiresPhase4Upgrade;

    boolean requiresTagChoice;

    boolean requiresMicrobeInput;
    boolean requiresAnimalInput;
    boolean requiresScienceResourceInput;

    boolean decomposersActive;
    boolean marsUniversityActive;
    boolean viralEnhancersActive;

    boolean requiresOxygenCheck;
    boolean requiresRedCard;
}
