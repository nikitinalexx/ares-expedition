package com.terraforming.ares.services.ai.dto;

import java.util.Map;

public record MctsDecision(
        int chosenPhase,
        float winnerAvgQ,
        Map<Integer, Float> policyTarget
) {}
