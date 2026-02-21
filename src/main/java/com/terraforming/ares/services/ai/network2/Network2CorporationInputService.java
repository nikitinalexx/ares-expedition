package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.network2.buildParams.OptimizedInputDecisions;
import com.terraforming.ares.services.ai.network2.buildParams.SharedInputAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.terraforming.ares.model.Constants.*;

/**
 * @author oleksii.nikitin
 * @since 1/17/2026
 */
@Service
@RequiredArgsConstructor
public class Network2CorporationInputService {

    private final IDataCollect iDataCollect;
    private final NNService nnService;

    public SharedInputAnalysis analyzeCorporationsForSharedInput(Set<CardAction> corporations) {
        SharedInputAnalysis sharedInputAnalysis = new SharedInputAnalysis();
        boolean requiresAnyInput = false;
        if (corporations.contains(CardAction.NEBU_LABS_CORPORATION)) {
            sharedInputAnalysis.requiresPhaseUpgrade = true;
            requiresAnyInput = true;
        }
        if (corporations.contains(CardAction.SULTIRA_CORPORATION)) {
            sharedInputAnalysis.requiresPhase1Upgrade = true;
            requiresAnyInput = true;
        }
        if (corporations.contains(CardAction.APOLLO_CORPORATION)) {
            sharedInputAnalysis.requiresPhase2Upgrade = true;
            requiresAnyInput = true;
        }
        if (corporations.contains(CardAction.HYPERION_SYSTEMS_CORPORATION)) {
            sharedInputAnalysis.requiresPhase3Upgrade = true;
            requiresAnyInput = true;
        }
        if (corporations.contains(CardAction.EXOCORP_CORPORATION)) {
            sharedInputAnalysis.requiresPhase5Upgrade = true;
            requiresAnyInput = true;
        }
        if (corporations.contains(CardAction.MODPRO_CORPORATION) || corporations.contains(CardAction.AUSTELLAR_CORPORATION)) {
            sharedInputAnalysis.requiresTagChoice = true;
            requiresAnyInput = true;
        }
        if (!requiresAnyInput) {
            return null;
        }
        return sharedInputAnalysis;
    }

    public Map<Integer, List<Integer>> getCorporationInput(MarsGame game, Player player, CardAction corporationCardAction, OptimizedInputDecisions inputDecisions) {
        if (corporationCardAction == CardAction.HYPERION_SYSTEMS_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(inputDecisions.getPhaseUpgradeDecisions().get(PERFORM_BLUE_ACTION_PHASE - 1).getValue()));
        } else if (corporationCardAction == CardAction.APOLLO_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(inputDecisions.getPhaseUpgradeDecisions().get(BUILD_BLUE_RED_PROJECTS_PHASE - 1).getValue()));
        } else if (corporationCardAction == CardAction.EXOCORP_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(inputDecisions.getPhaseUpgradeDecisions().get(DRAFT_CARDS_PHASE - 1).getValue()));
        } else if (corporationCardAction == CardAction.AUSTELLAR_CORPORATION) {
            List<float[]> statesWithAustellar = new ArrayList<>();
            for (int i = 0; i < game.getMilestones().size(); i++) {
                player.setAustellarMilestone(i);
                statesWithAustellar.add(iDataCollect.collectData(game, player.getUuid()));
            }
            player.setAustellarMilestone(-1);
            List<Prediction> predictions = nnService.predictBatch(statesWithAustellar, player);
            int bestMilestoneIndex = 0;
            double bestChance = predictions.getFirst().baseProb;
            for (int i = 1; i < predictions.size(); i++) {
                Prediction prediction = predictions.get(i);
                if (prediction.baseProb > bestChance) {
                    bestMilestoneIndex = i;
                    bestChance = prediction.baseProb;
                }
            }

            return Map.of(InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId(), List.of(bestMilestoneIndex), InputFlag.TAG_INPUT.getId(), List.of(inputDecisions.getBestTagDecision().getFirst().getValue().ordinal()));
        } else if (corporationCardAction == CardAction.MODPRO_CORPORATION) {
            return Map.of(InputFlag.TAG_INPUT.getId(), List.of(inputDecisions.getBestTagDecision().getFirst().getValue().ordinal()));
        } else if (corporationCardAction == CardAction.NEBU_LABS_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(inputDecisions.getBestPhaseUpgradeOverall().getValue()));
        } else if (corporationCardAction == CardAction.SULTIRA_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(inputDecisions.getPhaseUpgradeDecisions().get(BUILD_GREEN_PROJECTS_PHASE - 1).getValue()));
        }
        return Map.of();
    }

}
