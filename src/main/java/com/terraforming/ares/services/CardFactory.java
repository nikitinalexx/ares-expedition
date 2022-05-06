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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        inmemoryProjectCards = Stream.of(
                new AdaptationTechnology(1),
                new AdvancedAlloys(2),
                new AdvancedScreeningTechnology(3),
                new AiCentral(4),
                new AnaerobicMicroorganisms(5),
                new AntiGravityTechnology(6),
                new AquiferPumping(7),
                new ArcticAlgae(8),
                new ArtificialJungle(9),
                new AssemblyLines(10),
                new AssetLiquidation(11),
                new Birds(12),
                new BrainstormingSession(13),
                new CaretakerContract(14),
                new CircuitBoardFactory(15),
                new CommunityGardens(16),
                new CompostingFactory(17),
                new ConservedBiome(18),
                new Decomposers(19),
                new GeothermalPower(140)
        ).collect(Collectors.toMap(ProjectCard::getId, Function.identity()));
    }

    public Map<Integer, CorporationCard> createCorporations() {
        return inmemoryCorporationsStorage;
    }

    public Map<Integer, ProjectCard> createProjects() {
        return inmemoryProjectCards;
    }
}
