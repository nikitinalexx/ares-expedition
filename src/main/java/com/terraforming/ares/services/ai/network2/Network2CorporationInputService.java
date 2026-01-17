package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.ai.network2.buildParams.SharedInputAnalysis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.terraforming.ares.model.Constants.*;
import static com.terraforming.ares.model.Constants.BUILD_GREEN_PROJECTS_PHASE;

/**
 * @author oleksii.nikitin
 * @since 1/17/2026
 */
@Service
public class Network2CorporationInputService {

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
        if (corporations.contains(CardAction.MODPRO_CORPORATION)) {
            sharedInputAnalysis.requiresTagChoice = true;
            requiresAnyInput = true;
        }
        if (!requiresAnyInput) {
            return null;
        }
        return sharedInputAnalysis;
    }

    public Map<Integer, List<Integer>> getCorporationInput(MarsGame game, Player player, CardAction corporationCardAction) {
        if (corporationCardAction == CardAction.HYPERION_SYSTEMS_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player, PERFORM_BLUE_ACTION_PHASE)));
        } else if (corporationCardAction == CardAction.APOLLO_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player, BUILD_BLUE_RED_PROJECTS_PHASE)));
        } else if (corporationCardAction == CardAction.EXOCORP_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player, DRAFT_CARDS_PHASE)));
        } else if (corporationCardAction == CardAction.AUSTELLAR_CORPORATION) {
            return Map.of(InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId(), List.of(chooseAustellarCorporationMilestone(game, player)), InputFlag.TAG_INPUT.getId(), List.of(Tag.SCIENCE.ordinal()));
        } else if (corporationCardAction == CardAction.MODPRO_CORPORATION) {
            return Map.of(InputFlag.TAG_INPUT.getId(), List.of(chooseDynamicTagValue(player, List.of())));
        } else if (corporationCardAction == CardAction.NEBU_LABS_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player)));
        } else if (corporationCardAction == CardAction.SULTIRA_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player, BUILD_GREEN_PROJECTS_PHASE)));
        }
        return Map.of();
    }
}
