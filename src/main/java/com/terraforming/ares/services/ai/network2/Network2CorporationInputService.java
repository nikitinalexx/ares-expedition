package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.advanced.features.hand.HandEncoderHelperService;
import com.terraforming.ares.services.ai.advanced.features.hand.Stats;
import com.terraforming.ares.services.ai.network2.buildParams.OptimizedInputDecisions;
import com.terraforming.ares.services.ai.network2.buildParams.SharedInputAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
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
@RequiredArgsConstructor
public class Network2CorporationInputService {

    private final CardService cardService;
    private final HandEncoderHelperService handEncoderHelperService;

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
            List<Card> hand = player.getHand().getCards().stream().map(cardService::getCard).toList();
            Map<GainType, Stats> gainTypeToCounts = getTerraformingGains(hand);
            Map<GainType, Stats> incomeStats = getRequiredIncomeStats(hand);
            int[] handTagCounts = new int[11];
            for (Card card : hand) {
                List<Tag> tags = card.getTags();
                for (Tag tag : tags) {
                    handTagCounts[tag.ordinal()]++;
                }
            }

            float bestProgres = 0;
            int bestMilestoneIndex = 0;
            for (int i = 0; i < game.getMilestones().size(); i++) {
                Milestone milestone = game.getMilestones().get(i);
                float milestoneProgress = handEncoderHelperService.countMilestoneProgressByHand(milestone, hand, handTagCounts, gainTypeToCounts, incomeStats);
                if (milestoneProgress > bestProgres) {
                    bestProgres = milestoneProgress;
                    bestMilestoneIndex = i;
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

    private Map<GainType, Stats> getRequiredIncomeStats(List<Card> hand) {
        Stats heat = new Stats();
        Stats plant = new Stats();
        Stats steel = new Stats();

        for (Card c : hand) {
            List<Gain> incomes = c.getCardMetadata().getIncomes();
            if (incomes == null) continue;

            for (Gain g : incomes) {
                int v = g.getValue();
                switch (g.getType()) {
                    case HEAT -> {
                        heat.sum += v;
                    }
                    case PLANT -> {
                        plant.sum += v;
                    }
                    case STEEL -> {
                        steel.nonZero++;
                    }
                }
            }
        }

        Map<GainType, Stats> result = new EnumMap<>(GainType.class);
        result.put(GainType.HEAT, heat);
        result.put(GainType.PLANT, plant);
        result.put(GainType.STEEL, steel);
        return result;
    }

    private Map<GainType, Stats> getTerraformingGains(List<Card> hand) {
        Stats forest = new Stats();
        Stats oxygen = new Stats();
        Stats temperature = new Stats();
        Stats ocean = new Stats();
        Stats terraformingRating = new Stats();

        for (Card c : hand) {
            List<Gain> incomes = c.getCardMetadata().getBonuses();
            if (incomes == null) continue;

            for (Gain g : incomes) {
                int v = g.getValue();
                switch (g.getType()) {
                    case FOREST -> {
                        forest.sum += v;
                    }
                    case OXYGEN -> {
                        oxygen.sum += v;
                    }
                    case TEMPERATURE -> {
                        temperature.sum += v;
                    }
                    case OCEAN -> {
                        ocean.sum += v;
                    }
                    case TERRAFORMING_RATING -> {
                        terraformingRating.sum += v;
                    }
                }
            }
        }

        Map<GainType, Stats> result = new EnumMap<>(GainType.class);
        result.put(GainType.FOREST, forest);
        result.put(GainType.OXYGEN, oxygen);
        result.put(GainType.TEMPERATURE, temperature);
        result.put(GainType.OCEAN, ocean);
        result.put(GainType.TERRAFORMING_RATING, terraformingRating);
        return result;
    }


}
