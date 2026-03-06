package com.terraforming.ares.services.ai.dl4j;

import com.terraforming.ares.services.policyai.action.ActionHead;

public class PolicyPrediction {
    public float value;             // Оценка позиции (0...1)
    public float[] buildLogits;     // 268
    public float[] sellLogits;      // 268
    public float[] targetLogits;    // 90
    public float[] blueLogits;      // 49
    public float[] corpLogits;      // 25
    public float[] smallLogits;     // 40
    public float[] systemLogits;    // 6

    public PolicyPrediction() {
    }

    // Полезный метод для MCTS: получение вероятности конкретного действия
    public float getActionProbability(ActionHead head, int actionId) {
        switch (head) {
            case BUILD:
                return buildLogits[actionId];
            case SELL_DISCARD:
                return sellLogits[actionId];
            case TARGET:
                return targetLogits[actionId];
            case BLUE:
                return blueLogits[actionId];
            case CORPORATION:
                return corpLogits[actionId];
            case SMALL:
                return smallLogits[actionId];
            case SYSTEM:
                return systemLogits[actionId];
            default:
                return 0f;
        }
    }
}
