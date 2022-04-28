package com.terraforming.ares.services;

import com.terraforming.ares.cards.corporations.CelestiorCorporation;
import com.terraforming.ares.cards.corporations.DevTechs;
import com.terraforming.ares.cards.corporations.HelionCorporation;
import com.terraforming.ares.cards.corporations.LaunchStarIncorporated;
import com.terraforming.ares.model.CorporationCard;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Expansion;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class DeckService {
    private final Map<Integer, CorporationCard> inmemoryCorporationsStorage = Map.of(
            1, new HelionCorporation(),
            2, new CelestiorCorporation(),
            3, new DevTechs(),
            4, new LaunchStarIncorporated()
    );

    public Deck createProjectsDeck(List<Expansion> expansions) {
        //TODO logic that builds a deck based on list of selected expansions
        return Deck.builder()
                .cards(new LinkedList<>(
                        IntStream.range(1, 21).boxed().collect(Collectors.toList())
                ))
                .build();
    }

    public Deck createCorporationsDeck(List<Expansion> expansions) {
        //TODO logic that builds a deck based on list of selected expansions
        return Deck.builder()
                .cards(new LinkedList<>(
                        IntStream.range(1, 5).boxed().collect(Collectors.toList())
                ))
                .build();
    }

    public CorporationCard getCard(int id) {
        return inmemoryCorporationsStorage.get(id);
    }

}
