package com.terraforming.ares.model;

import lombok.Builder;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
@Getter
public class Deck {
    private final LinkedList<Integer> cards;

    public Deck dealCards(int count) {
        LinkedList<Integer> result = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            result.add(cards.remove(0));
        }
        return Deck.builder().cards(result).build();
    }

    public void removeCards(List<Integer> cards) {
        this.cards.removeAll(cards);
    }

    public boolean containsCard(int cardId) {
        return cards.contains(cardId);
    }

    public void addCard(Integer card) {
        cards.add(card);
    }

}
