package com.terraforming.ares.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Deck {
    @Builder.Default
    private final LinkedList<Integer> cards = new LinkedList<>();

    public Deck dealCardsDeck(int count) {
        LinkedList<Integer> result = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            result.add(cards.remove(0));
        }
        return Deck.builder().cards(result).build();
    }

    public List<Integer> dealCards(int count) {
        List<Integer> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(cards.remove(0));
        }
        return result;
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

    public void addCards(List<Integer> cards) {
        this.cards.addAll(cards);
    }

    public int size() {
        return cards.size();
    }

}
