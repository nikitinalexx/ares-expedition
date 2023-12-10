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
        int blueCards = 0;
        int redCards = 0;
        int greenCards = 0;
        for (Integer cardId : player.getPlayed().getCards()) {
            Card card = cardService.getCard(cardId);
            if (card.getColor() == CardColor.BLUE) {
                blueCards++;
            } else if (card.getColor() == CardColor.GREEN) {
                greenCards++;
            } else if (card.getColor() == CardColor.RED) {
                redCards++;
            }
        }
        return Math.min(blueCards, Math.min(redCards, greenCards));
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
