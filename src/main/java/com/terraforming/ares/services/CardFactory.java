package com.terraforming.ares.services;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.*;
import com.terraforming.ares.cards.corporations.*;
import com.terraforming.ares.cards.green.*;
import com.terraforming.ares.cards.red.*;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Expansion;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Service
public class CardFactory {
    private final Map<Integer, Card> corporationsStorage;
    private final Map<Integer, Card> buffedCorporationsStorage;
    private final Map<Integer, Card> buffedCorporationsMapping;
    private final List<Card> sortedCorporations;
    private final List<Card> buffedCorporations;

    private final Map<Integer, Card> baseExpansionProjects;
    private final Map<Integer, Card> discoveryExpansionProjects;

    //used for display of all projecst cards, needs sorting in advance
    private final List<Card> baseExpansionSortedProjects;
    private final List<Card> discoveryExpansionSortedProjects;

    public CardFactory() {
        baseExpansionSortedProjects = List.of(
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
                new DecomposingFungus(20),
                new DevelopedInfrastructure(21),
                new DevelopmentCenter(22),
                new EarthCatapult(23),
                new EcologicalZone(24),
                new EnergySubsidies(25),
                new ExtendedResources(26),
                new ExtremeColdFungus(27),
                new FarmersMarket(28),
                new FarmingCoops(29),
                new Fish(30),
                new GhgProductionBacteria(31),
                new GreenHouses(32),
                new Herbivores(33),
                new HydroElectricEnergy(34),
                new InterplanetaryRelations(35),
                new Interns(36),
                new InterplanetaryConference(37),
                new IronWorks(38),
                new Livestock(39),
                new MarsUniversity(40),
                new MatterManufactoring(41),
                new MediaGroup(42),
                new NitriteReductingBacteria(43),
                new OlympusConference(44),
                new OptimalAerobraking(45),
                new PhysicsComplex(46),
                new PowerInfrastructure(47),
                new RecycledDetritus(48),
                new RedraftedContracts(49),
                new RegolithEaters(50),
                new ResearchOutpost(51),
                new SmallAnimals(52),
                new RestructuredResources(53),
                new SolarPunk(54),
                new StandardTechnology(55),
                new Steelworks(56),
                new SymbioticFungus(57),
                new Tardigrades(58),
                new ThinkTank(59),
                new UnitedPlanetaryAlliance(60),
                new ViralEnhancers(61),
                new VolcanicPools(62),
                new WaterImportFromEuropa(63),
                new WoodBurningStoves(64),
                new AdvancedEcosystems(65),
                new ArtificialLake(66),
                new AtmosphereFiltering(67),
                new BreathingFilters(68),
                new BribedComittee(69),
                new BusinessContracts(70),
                new CeosFavoriteProject(71),
                new ColonizerTrainingCamp(72),
                new Comet(73),
                new ConvoyFromEuropa(74),
                new Crater(75),
                new DeimosDown(76),
                new GiantIceAsteroid(77),
                new IceAsteroid(78),
                new IceCapMelting(79),
                new ImportedHydrogen(80),
                new ImportedNitrogen(81),
                new InterstellarColonyShip(82),
                new InventionContest(83),
                new InvestmentLoan(84),
                new LagrangeObservatory(85),
                new LakeMariners(86),
                new LargeConvoy(87),
                new LavaFlows(88),
                new LocalHeatTrapping(89),
                new Mangrove(90),
                new NitrogenRichAsteroid(91),
                new PermafrostExtraction(92),
                new PhobosFalls(93),
                new Plantation(94),
                new ReleaseInertGases(95),
                new Research(96),
                new SpecialDesign(97),
                new SubterraneanReservoir(98),
                new TechnologyDemonstration(99),
                new TerraformingGanymede(100),
                new TowingAComet(101),
                new WorkCrews(102),
                new AcquiredCompany(103),
                new AdaptedLichen(104),
                new AeratedMagma(105),
                new AirborneRadiation(106),
                new Algae(107),
                new Archaebacteria(108),
                new ArtificialPhotosynthesis(109),
                new AsteroidMining(110),
                new AsteroidMiningConsortium(111),
                new Astrofarm(112),
                new AtmosphericInsulators(113),
                new AutomatedFactories(114),
                new BalancedPortfolios(115),
                new BeamFromThoriumAsteroid(116),
                new BiomassCombustors(117),
                new BiothermalPower(118),
                new Blueprints(119),
                new BuildingIndustries(120),
                new Bushes(121),
                new CallistoPenalMines(122),
                new Cartel(123),
                new CoalImports(124),
                new CommercialDistrict(125),
                new DeepWellHeating(126),
                new DesignedMicroorganisms(127),
                new DiversifiedInterests(128),
                new DustyQuarry(129),
                new EconomicGrowth(130),
                new EnergyStorage(131),
                new EosChasmaNationalPark(132),
                new Farming(133),
                new FoodFactory(134),
                new FuelFactory(135),
                new FueledGenerators(136),
                new FusionPower(137),
                new GanymedeShipyard(138),
                new GeneRepair(139),
                new GeothermalPower(140),
                new GiantSpaceMirror(141),
                new Grass(142),
                new GreatDam(143),
                new GreatEscarpmentConsortium(144),
                new Heather(145),
                new ImmigrationShuttles(146),
                new ImportOfAdvancedGHG(147),
                new ImportedGHG(148),
                new IndustrialCenter(149),
                new IndustrialFarming(150),
                new IndustrialMicrobes(151),
                new Insects(152),
                new IoMiningIndustries(153),
                new KelpFarming(154),
                new Lichen(155),
                new LightningHarvest(156),
                new LowAtmoShields(157),
                new LunarBeam(158),
                new MassConverter(159),
                new MedicalLab(160),
                new MethaneFromTitan(161),
                new MicroMills(162),
                new Microprocessors(163),
                new Mine(164),
                new MirandaResort(165),
                new MoholeArea(166),
                new Monocultures(167),
                new Moss(168),
                new NaturalPreserve(169),
                new NewPortfolios(170),
                new NitropholicMoss(171),
                new NoctisFarming(172),
                new NuclearPlants(173),
                new PowerGrid(174),
                new PowerPlant(175),
                new PowerSupplyConsortium(176),
                new ProtectedValley(177),
                new QuantumExtractor(178),
                new RadSuits(179),
                new SatelliteFarms(180),
                new Satellites(181),
                new SlashAndBurnAgriculture(182),
                new Smelting(183),
                new SoilWarming(184),
                new SolarPower(185),
                new SolarTrapping(186),
                new Soletta(187),
                new SpaceHeater(188),
                new SpaceStation(189),
                new Sponsors(190),
                new StripMine(191),
                new SurfaceMines(192),
                new TectonicStressPower(193),
                new TitaniumMine(194),
                new TallStation(195),
                new TradingPost(196),
                new TrappedHeat(197),
                new Trees(198),
                new TropicalResort(199),
                new TundraFarming(200),
                new UndergroundCity(201),
                new UnderseaVents(202),
                new VentureCapitalism(203),
                new VestaShipyard(204),
                new WavePower(205),
                new Windmills(206),
                new Worms(207),
                new Zeppelins(208),
                new AssortedEnterprises(209),
                new CommercialImports(210),
                new DiverseHabitats(211),
                new Laboratories(212),
                new MatterGenerator(213),
                new ProcessedMetals(214),
                new ProcessingPlant(215),
                new ProgressivePolicies(216),
                new FilterFeeders(217),
                new SyntheticCatastrophe(218),
                new SelfReplicatingBacteria(219)
        );

        discoveryExpansionSortedProjects = List.of(
                new CommunicationsStreamlining(305),
                new DroneAssistedConstruction(306),
                new ExperimentalTechnology(307),
                new ImpactAnalysis(308),
                new OreLeaching(318),

                new BiomedicalImports(314),
                new CryogenicShipment(315),
                new Exosuits(316),
                new ImportedConstructionCrews(317),
                new PrivateInvestorBeach(319),
                new TopographicMapping(320),
                new ThreeDPrinting(321),
                new Biofoundries(322),
                new BlastFurnaces(323),
                new Dandelions(324),
                new ElectricArcFurnaces(325),
                new LocalMarket(326),
                new ManufacturingHub(327),
                new HeatReflectiveGlass(328),
                new HematiteMining(329),
                new HydroponicGardens(330),
                new IlmeniteDeposits(331),
                new IndustrialComplex(332),
                new MartianMuseum(333),
                new Metallurgy(334),
                new AwardWinningReflectorMaterial(335),
                new PerfluorocarbonProduction(337),
                new MagneticFieldGenerator(338),
                new PoliticalInfluence(339),
                new BiologicalFactories(340),
                new NuclearDetonationSite(341),
                new Warehouses(342),

                new MartianStudiesScholarship(377)
        );

        sortedCorporations = List.of(
                new HelionCorporation(10000),
                new CelestiorCorporation(10001),
                new DevTechs(10002),
                new LaunchStarIncorporated(10003),
                new ThorgateCorporation(10004),
                new TeractorCorporation(10005),
                new TharsisCorporation(10006),
                new CredicorCorporation(10007),
                new ArclightCorporation(10008),
                new PhobologCorporation(10009),
                new MiningGuildCorporation(10010),
                new SaturnSystemsCorporation(10011),
                new ZetacellCorporation(10012),
                new EcolineCorporation(10013),
                new Inventrix(10014),
                new MayNiProductionsCorporation(10015),
                new UnmiCorporation(10016),
                new InterplanetaryCinematics(10017)
        );

        buffedCorporationsMapping = Map.of(
                10000, new BuffedHelionCorporation(10100),
                10008, new BuffedArclightCorporation(10101),
                10009, new BuffedPhobologCorporation(10102),
                10010, new BuffedMiningGuildCorporation(10103),
                10011, new BuffedSaturnSystemsCorporation(10104),
                10012, new BuffedZetacellCorporation(10105),
                10013, new BuffedEcolineCorporation(10106),
                10014, new BuffedInventrix(10107),
                10016, new BuffedUnmiCorporation(10108)
        );

        buffedCorporations = new ArrayList<>(buffedCorporationsMapping.values());

        baseExpansionProjects = baseExpansionSortedProjects.stream().collect(Collectors.toMap(Card::getId, Function.identity()));
        discoveryExpansionProjects = discoveryExpansionSortedProjects.stream().collect(Collectors.toMap(Card::getId, Function.identity()));
        corporationsStorage = sortedCorporations.stream().collect(Collectors.toMap(Card::getId, Function.identity()));
        buffedCorporationsStorage = buffedCorporations.stream().collect(Collectors.toMap(Card::getId, Function.identity()));
    }

    public Map<Integer, Card> createCorporations() {
        return corporationsStorage;
    }

    public Map<Integer, Card> createBuffedCorporations() {
        return buffedCorporationsStorage;
    }

    public Map<Integer, Card> getBuffedCorporationsMapping() {
        return buffedCorporationsMapping;
    }

    public Map<Expansion, Map<Integer, Card>> createAllProjects() {
        return Map.of(Expansion.BASE, baseExpansionProjects,
                Expansion.DISCOVERY, discoveryExpansionProjects
        );
    }

    public List<Card> getAllProjects(List<Expansion> expansions) {
        //todo refactor remove if checks, make a map expansion -> cards
        return Stream.concat(
                expansions.contains(Expansion.BASE) ?
                        baseExpansionSortedProjects.stream().sorted(Comparator.comparingInt(
                                card -> card.getColor().ordinal()
                        )) : Stream.empty(),
                expansions.contains(Expansion.DISCOVERY) ?
                        discoveryExpansionSortedProjects.stream().sorted(Comparator.comparingInt(
                                card -> card.getColor().ordinal()
                        )) : Stream.empty()
        ).collect(Collectors.toList());
    }

    public List<Card> getAllCorporations(List<Expansion> expansions) {
        Map<Integer, Card> corporations = new HashMap<>();

        if (expansions.contains(Expansion.BASE)) {
            corporations.putAll(corporationsStorage);
        }

        if (expansions.contains(Expansion.BUFFED_CORPORATION)) {
            corporations.putAll(buffedCorporationsMapping);
        }

        return new ArrayList<>(corporations.values());
    }

}
