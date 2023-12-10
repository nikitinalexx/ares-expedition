package com.terraforming.ares.model.milestones;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class TriadMasteryMilestone extends Milestone {

    @Override
    public boolean isAchievable(Player player, CardService cardService) {
        return countSetsOfCards(player, cardService) >= getMaxValue();
    }

    @Override
    public int getValue(Player player, CardService cardService) {
        return Math.min(countSetsOfCards(player, cardService), getMaxValue());
    }

    private int countSetsOfCards(Player player, CardService cardService) {
        return player.getPlayed().getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> card.getColor() != CardColor.CORPORATION)
                .collect(Collectors.groupingBy(Card::getColor, Collectors.counting()))
                .values()
                .stream()
                .mapToInt(Long::intValue)
                .min()
                .orElse(0);
    }

    @Override
    public MilestoneType getType() {
        return MilestoneType.TRIAD_MASTERY;
    }

    @Override
    public int getMaxValue() {
        return 3;
    }
}
