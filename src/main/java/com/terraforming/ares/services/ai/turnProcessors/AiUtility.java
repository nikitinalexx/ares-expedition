package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiUtility {
    private final CardService cardService;

    public List<Card> getPlayerCardsWithResource(Player player, Set<CardCollectableResource> resources) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> resources.contains(card.getCollectableResource()))
                .collect(Collectors.toList());
    }

    public void simulateDummyHandInsteadOfNewCards(Player player, int originalPlayerHandSize) {
        int newCardsGained = player.getHand().size() - originalPlayerHandSize;

        if (newCardsGained > 0) {
            List<Integer> newHand = player.getHand().getCards();
            newHand.subList(newHand.size() - newCardsGained, newHand.size()).clear();
            for (int i = 0; i < newCardsGained; i++) {
                newHand.add(AiConstants.GENERIC_DUMMY_ID);
            }
        }
    }

    public void addDummyCards(Player player, int count) {
        if (count > 0) {
            List<Integer> newHand = player.getHand().getCards();
            for (int i = 0; i < count; i++) {
                newHand.add(AiConstants.GENERIC_DUMMY_ID);
            }
        }
    }

}
