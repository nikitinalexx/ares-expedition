package com.terraforming.ares.services.ai.hand;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.awards.BaseAward;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.milestones.Milestone;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HandEncoderHelperService {

    public float countMilestoneProgressByHand(Milestone milestone, Set<Card> hand, int[] handTagCounts, Map<GainType, FeatureStats> gainTypeToCounts, Map<GainType, FeatureStats> incomeTypeToCounts) {
        float result = 0;
        switch (milestone.getType()) {
            case MAGNATE -> result = hand.stream().filter(card -> card.getColor() == CardColor.GREEN).count() / 8f;
            case TERRAFORMER ->//requires 15TR
                    result = (gainTypeToCounts.get(GainType.FOREST).sum + gainTypeToCounts.get(GainType.OXYGEN).sum + gainTypeToCounts.get(GainType.TEMPERATURE).sum + gainTypeToCounts.get(GainType.OCEAN).sum + gainTypeToCounts.get(GainType.TERRAFORMING_RATING).sum) / 15f;
            case BUILDER -> result = handTagCounts[Tag.BUILDING.ordinal()] / 8f;
            case SPACE_BARON -> result = handTagCounts[Tag.SPACE.ordinal()] / 7f;
            case ENERGIZER -> result = incomeTypeToCounts.get(GainType.HEAT).sum / 10f;
            case FARMER -> result = incomeTypeToCounts.get(GainType.PLANT).sum / 5f;
            case TYCOON -> result = hand.stream().filter(card -> card.getColor() == CardColor.BLUE).count() / 6f;
            case PLANNER -> {//has built 12 cards
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

    public float countAwardProgressByHand(BaseAward baseAward, Set<Card> hand, int[] handTagCounts) {
        return switch (baseAward.getType()) {
            case CELEBRITY -> extractTotalIncome(hand, Set.of(GainType.MC));
            case GENERATOR -> extractTotalIncome(hand, Set.of(GainType.HEAT));
            case FLORA_HARVEST, GARDENER -> extractTotalIncome(hand, Set.of(GainType.PLANT));
            case INDUSTRIALIST -> extractTotalIncome(hand, Set.of(GainType.STEEL, GainType.TITANIUM));
            case RESEARCHER -> handTagCounts[Tag.SCIENCE.ordinal()];
            case BOTANIST -> handTagCounts[Tag.MICROBE.ordinal()] + handTagCounts[Tag.PLANT.ordinal()];
            case BUILDER -> handTagCounts[Tag.BUILDING.ordinal()];
            case PROJECT_MANAGER -> extractTotalIncome(hand, Set.of(GainType.CARD));
            case COLLECTOR -> handTagCounts[Tag.ANIMAL.ordinal()] + handTagCounts[Tag.MICROBE.ordinal()];
            case CRITERION -> countCardsWithRequirements(hand);
        };
    }

    private int extractTotalIncome(Set<Card> hand, Set<GainType> gainType) {
        return hand.stream().mapToInt(card -> {
            CardMetadata cardMetadata = card.getCardMetadata();
            if (cardMetadata == null) {
                return 0;
            }
            List<Gain> incomes = cardMetadata.getIncomes();
            if (incomes == null) {
                return 0;
            }
            int total = 0;
            for (Gain income : incomes) {
                if (gainType.contains(income.getType())) {
                    total += income.getValue();
                }
            }
            return total;

        }).sum();
    }

    private int countCardsWithRequirements(Set<Card> cards) {
        int count = 0;
        for (Card card : cards) {
            if (card.getOceanRequirement() != null || !card.getTemperatureRequirement().isEmpty() || !card.getOxygenRequirement().isEmpty()) {
                count++;
            }
        }
        return count;
    }

}
