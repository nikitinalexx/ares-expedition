package com.alex.nikitin.terraformingm.model;

import lombok.Builder;
import lombok.Getter;

import java.util.LinkedList;

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
            result.add(cards.remove(i));
        }
        return Deck.builder().cards(result).build();
    }

}
