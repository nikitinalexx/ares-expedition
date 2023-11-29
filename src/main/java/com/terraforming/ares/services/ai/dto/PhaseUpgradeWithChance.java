package com.terraforming.ares.services.ai.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PhaseUpgradeWithChance {
    private float chance;
    private int phase;
    private int upgrade;

    public static PhaseUpgradeWithChance of(float chance, int phase, int upgrade) {
        return new PhaseUpgradeWithChance(chance, phase, upgrade);
    }

    public int getUpgradeAsNumber() {
        return phase * 2 + upgrade - 1;
    }

}
