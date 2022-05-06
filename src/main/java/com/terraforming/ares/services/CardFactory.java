package com.terraforming.ares.services;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.corporations.CelestiorCorporation;
import com.terraforming.ares.cards.corporations.DevTechs;
import com.terraforming.ares.cards.corporations.HelionCorporation;
import com.terraforming.ares.cards.corporations.LaunchStarIncorporated;
import com.terraforming.ares.cards.green.GeothermalPower;
import com.terraforming.ares.model.CorporationCard;
import com.terraforming.ares.model.ProjectCard;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Service
public class CardFactory {
    //TODO do something with these ids
    private final Map<Integer, CorporationCard> inmemoryCorporationsStorage = Map.of(
            1, new HelionCorporation(1),
            2, new CelestiorCorporation(2),
            3, new DevTechs(3),
            4, new LaunchStarIncorporated(4)
    );

    private final Map<Integer, ProjectCard> inmemoryProjectCards;

    public CardFactory() {
        Map<Integer, ProjectCard> tempMap = new HashMap<>();
        tempMap.put(1, new AdaptationTechnology(1));
        tempMap.put(2, new AdvancedAlloys(2));
        tempMap.put(3, new AdvancedScreeningTechnology(3));
        tempMap.put(4, new AiCentral(4));
        tempMap.put(5, new AnaerobicMicroorganisms(5));
        tempMap.put(6, new AntiGravityTechnology(6));
        tempMap.put(7, new AquiferPumping(7));
        tempMap.put(8, new ArcticAlgae(8));
        tempMap.put(9, new ArtificialJungle(9));
        tempMap.put(10, new AssemblyLines(10));
        tempMap.put(11, new AssetLiquidation(11));
        tempMap.put(140, new GeothermalPower(140));

        inmemoryProjectCards = Map.copyOf(tempMap);
    }

    public Map<Integer, CorporationCard> createCorporations() {
        return inmemoryCorporationsStorage;
    }

    public Map<Integer, ProjectCard> createProjects() {
        return inmemoryProjectCards;
    }
}
