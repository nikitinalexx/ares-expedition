package com.terraforming.ares.services;

import com.terraforming.ares.cards.blue.AiCentral;
import com.terraforming.ares.cards.blue.AnaerobicMicroorganisms;
import com.terraforming.ares.cards.corporations.CelestiorCorporation;
import com.terraforming.ares.cards.corporations.DevTechs;
import com.terraforming.ares.cards.corporations.HelionCorporation;
import com.terraforming.ares.cards.corporations.LaunchStarIncorporated;
import com.terraforming.ares.cards.green.GeothermalPower;
import com.terraforming.ares.model.CorporationCard;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.ProjectCard;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
    //TODO do something with these ids
    private final Map<Integer, CorporationCard> inmemoryCorporationsStorage = Map.of(
            1, new HelionCorporation(1),
            2, new CelestiorCorporation(2),
            3, new DevTechs(3),
            4, new LaunchStarIncorporated(4)
    );

    private final Map<Integer, ProjectCard> inmemoryProjectCards;

    public DeckService() {
        Map<Integer, ProjectCard> tempMap = new HashMap<>();
        for (int i = 0; i <= 20; i++) {
            tempMap.put(i, new GeothermalPower(i));
        }
        tempMap.put(21, new AnaerobicMicroorganisms(21));
        tempMap.put(22, new AiCentral(22));

        inmemoryProjectCards = Map.copyOf(tempMap);
    }

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

    public CorporationCard getCorporationCard(int id) {
        return inmemoryCorporationsStorage.get(id);
    }

    public ProjectCard getProjectCard(int id) {
        return inmemoryProjectCards.get(id);
    }

}
