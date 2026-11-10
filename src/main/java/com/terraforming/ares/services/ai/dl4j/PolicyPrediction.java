package com.terraforming.ares.services.ai.dl4j;

import com.terraforming.ares.services.policyai.action.ActionHead;

public class PolicyPrediction {
    public float outcome;             // Оценка позиции (0...1)
    public float[] buildArgmax;     // 268
    public float[] sellArgmax;      // 268
    public float[] targetArgmax;    // 90
    public float[] blueActionsArgmax;      // 49
    public float[] corporationArgmax;      // 25
    public float[] smallActionsArgmax;     // 40
    public float[] systemActionsArgmax;    // 6
    public float[] headSelArgmax; //7

    public PolicyPrediction() {
    }

    // Полезный метод для MCTS: получение вероятности конкретного действия
    public float getActionProbability(ActionHead head, int actionId) {
        switch (head) {
            case BUILD:
                return buildArgmax[actionId];
            case SELL_DISCARD:
                return sellArgmax[actionId];
            case TARGET:
                return targetArgmax[actionId];
            case BLUE:
                return blueActionsArgmax[actionId];
            case CORPORATION:
                return corporationArgmax[actionId];
            case SMALL:
                return smallActionsArgmax[actionId];
            case SYSTEM:
                return systemActionsArgmax[actionId];
            case HEAD_SELECTION:
                return headSelArgmax[actionId];
            default:
                return 0f;
        }
    }
}
