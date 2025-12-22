package com.terraforming.ares.services.ai.hand;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.milestones.Milestone;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HandEncoderHelperService {
    
    public float countMilestoneProgressByHand(MarsGame game, Player player, Set<Card> hand, int[] handTagCounts, Map<GainType, FeatureStats> gainTypeToCounts, Map<GainType, FeatureStats> incomeTypeToCounts) {
        List<Milestone> milestones = game.getMilestones();
        
        if (player.getAustellarMilestone() < 0) {
            return 0f;
        }
        float result = 0;

        Milestone milestone = milestones.get(player.getAustellarMilestone());
        switch (milestone.getType()) {
            case MAGNATE -> result = hand.stream().filter(card -> card.getColor() == CardColor.GREEN).count() / 8f;
            case TERRAFORMER ->//15TR TODO check other cards that may affect terraformer
                    result = (gainTypeToCounts.get(GainType.FOREST).sum + gainTypeToCounts.get(GainType.OXYGEN).sum + gainTypeToCounts.get(GainType.TEMPERATURE).sum + gainTypeToCounts.get(GainType.OCEAN).sum + gainTypeToCounts.get(GainType.TERRAFORMING_RATING).sum) / 15f;
            case BUILDER -> result = handTagCounts[Tag.BUILDING.ordinal()] / 8f;
            case SPACE_BARON -> result = handTagCounts[Tag.SPACE.ordinal()] / 7f;
            case ENERGIZER -> result = incomeTypeToCounts.get(GainType.HEAT).sum / 10f;
            case FARMER -> result = incomeTypeToCounts.get(GainType.PLANT).sum / 5f;
            case TYCOON -> result = hand.stream().filter(card -> card.getColor() == CardColor.BLUE).count() / 6f;
            case PLANNER -> {
                int buildable = 0;
                for (Card card : hand) {
                    if (card.getPrice() <= 12 && card.getOxygenRequirement().isEmpty() && card.getTemperatureRequirement().isEmpty() && card.getOceanRequirement() == null) {
                        buildable++;
                    }
                }
                result = buildable / 12f;
            }
            case DIVERSIFIER -> {
                int differentTagsCount = 0;
                for (int handTagCount : handTagCounts) {
                    if (handTagCount > 0) {
                        differentTagsCount++;
                    }
                }
                result = differentTagsCount / 9f;
            }
            case LEGEND -> result = hand.stream().filter(card -> card.getColor() == CardColor.RED).count() / 6f;
            case GARDENER ->
                    result = Math.min((incomeTypeToCounts.get(GainType.PLANT).sum + gainTypeToCounts.get(GainType.FOREST).sum), 3) / 3f;
            case ECOLOGIST ->
                    result = (handTagCounts[Tag.MICROBE.ordinal()] + handTagCounts[Tag.PLANT.ordinal()] + handTagCounts[Tag.ANIMAL.ordinal()]) / 5f;
            case MINIMALIST -> {
                float handWeight = 0;
                for (Card card : hand) {
                    if (card.getColor() == CardColor.RED) {
                        handWeight += 0.1f;
                    } else if (card.getPrice() <= 10) {
                        handWeight += 0.3f;
                    } else {
                        handWeight += 0.8f;
                    }
                }
                result = Math.max(0f, 1.0f - (handWeight / 8f));
            }
            case TERRAN -> result = handTagCounts[Tag.EARTH.ordinal()] / 6f;
            case TRIAD_MASTERY -> {
                int blueCards = 0;
                int greenCards = 0;
                int redCards = 0;
                for (Card card : hand) {
                    if (card.getColor() == CardColor.BLUE) {
                        blueCards++;
                    }
                    if (card.getColor() == CardColor.GREEN) {
                        greenCards++;
                    }
                    if (card.getColor() == CardColor.RED) {
                        redCards++;
                    }
                }
                result = (Math.min(3, blueCards) / 3f + Math.min(3, greenCards) / 3f + Math.min(3, redCards) / 3f) / 3f;
            }
        }
        return Math.min(1.0f, result);
    }
}
