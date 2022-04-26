package com.terraforming.ares.services;

import com.terraforming.ares.model.Deck;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class MarsDeckService {

    public Deck createProjectsDeck() {
        return Deck.builder()
                .cards(new LinkedList<>(
                        IntStream.range(1, 21).boxed().collect(Collectors.toList())
                ))
                .build();
    }

    public Deck createCorporationsDeck() {
        return Deck.builder()
                .cards(new LinkedList<>(
                        IntStream.range(1, 11).boxed().collect(Collectors.toList())
                ))
                .build();
    }

}
