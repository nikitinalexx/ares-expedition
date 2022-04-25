package com.terraforming.ares.services;

import com.terraforming.ares.model.Deck;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class MarsDeckService {

    public Deck createProjectsDeck() {
        return Deck.builder().build();
    }

    public Deck createCorporationsDeck() {
        return Deck.builder().build();
    }

}
