package com.terraforming.ares.services.ai.network2.buildParams;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class SharedInputAnalysis {
    public boolean requiresPhaseUpgrade;
    public boolean requiresPhase1Upgrade;
    public boolean requiresPhase2Upgrade;
    public boolean requiresPhase3Upgrade;
    public boolean requiresPhase4Upgrade;
    public boolean requiresPhase5Upgrade;

    public boolean requiresTagChoice;

    public boolean requiresMicrobeInput;
    public Set<Integer> microbeTargets;
    public boolean requiresAnimalInput;
    public Set<Integer> animalTargets;
    public boolean requiresScienceResourceInput;
    public Set<Integer> scienceResourceTargets;

    public boolean decomposersActive;
    public boolean marsUniversityActive;
    public boolean viralEnhancersActive;

    public boolean requiresOxygenCheck;
    public List<Integer> redCardTargets;

    public int phaseUpgradeCounter;
}
