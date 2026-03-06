package com.terraforming.ares.services.policyai.service;

import com.terraforming.ares.services.ai.dl4j.PolicyPrediction;
import com.terraforming.ares.services.policyai.action.ActionHead;
import com.terraforming.ares.services.policyai.action.HeadAction;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyActionSelector {

    public  HeadAction chooseBestAction(
            PolicyPrediction prediction,
            List<HeadAction> legalActions
    ) {
        if (legalActions == null || legalActions.isEmpty()) {
            return null;
        }

        HeadAction best = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        ActionHead firstHead = null;
        boolean multipleHeads = false;

        for (int i = 0; i < legalActions.size(); i++) {
            HeadAction action = legalActions.get(i);

            ActionHead head = action.head();

            if (firstHead == null) {
                firstHead = head;
            } else if (!multipleHeads && head != firstHead) {
                multipleHeads = true;

                // ВАЖНО:
                // Мы уже сравнивали предыдущие действия без scaling.
                // Нужно пересчитать лучший score с scaling.
                bestScore *= headSize(firstHead);
            }

            float prob = prediction.getActionProbability(
                    head,
                    action.localIndex()
            );

            float score = multipleHeads
                    ? prob * headSize(head)
                    : prob;

            if (score > bestScore) {
                bestScore = score;
                best = action;
            }
        }

        return best;
    }

    private static int headSize(ActionHead head) {
        return switch (head) {
            case BUILD -> 268;
            case SELL_DISCARD -> 268;
            case TARGET -> 90;
            case BLUE -> 49;
            case CORPORATION -> 25;
            case SMALL -> 40;
            case SYSTEM -> 6;
        };
    }
}
