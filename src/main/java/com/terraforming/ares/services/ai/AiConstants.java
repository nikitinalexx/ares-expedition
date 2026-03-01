package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.corporations.CelestiorCorporation;
import com.terraforming.ares.cards.corporations.GenericDummyCard;
import com.terraforming.ares.cards.corporations.HyperionSystemsCorporation;
import com.terraforming.ares.cards.corporations.ModproCorporation;
import com.terraforming.ares.cards.green.*;
import com.terraforming.ares.cards.red.*;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.awards.AwardType;
import com.terraforming.ares.model.milestones.MilestoneType;
import com.terraforming.ares.validation.action.FibrousCompositeActionValidator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public final class AiConstants {
    public static final boolean POLICY_TEACHING = true;
    public static final boolean ENABLE_AI_EXPLORATION = true;
    public static final boolean ENABLE_SELL_AND_HEAT_EXPLORATION = true;
    public static final boolean EXPLORATION_ON_CORP_PICK = true;

    public static final boolean COLLECT_CARD_RANK_STATS = false;

    public static final int BACTERIAL_AGGREGATES_COUNT_FOR_MICROBE_PUT = 4;
    public static final int TABLE_VECTOR_SIZE = 417;
    public static final int HAND_VECTOR_SIZE = 481;
    public static final int HAND_BIT_MASKS_OFFSET = 534;

    public static final int TOTAL_SIZE = TABLE_VECTOR_SIZE + HAND_VECTOR_SIZE;


    public static final float[] NORMALIZATION_VECTOR = new float[]{
            1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 4.3307333f, 4.3307333f, 1.0f, 4.465908f, 4.465908f, 1.0f, 4.0430512f, 4.0430512f, 1.0f, 2.944439f, 2.944439f, 1.0f, 4.0775375f, 4.0775375f, 1.0f, 2.8903718f, 2.8903718f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 52.0f, 20.0f, 13.0f, 5.666667f, 100.0f, 52.0f, 121.0f, 13.0f, 11.0f, 30.0f, 56.0f, 10.0f, 393.0f, 83.0f, 200.0f, 58.0f, 25.0f, 31.0f, 34.0f, 20.0f, 14.0f, 19.0f, 18.0f, 14.0f, 16.0f, 27.0f, 7.0f, 9.0f, 15.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 45.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 23.0f, 48.0f, 31.0f, 26.0f, 38.0f, 39.0f, 36.0f, 36.0f, 19.0f, 42.0f, 30.0f, 34.0f, 36.0f, 33.0f, 15.0f, 60.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 52.0f, 20.0f, 13.0f, 5.666667f, 100.0f, 52.0f, 121.0f, 13.0f, 11.0f, 30.0f, 56.0f, 10.0f, 393.0f, 83.0f, 200.0f, 58.0f, 25.0f, 31.0f, 34.0f, 20.0f, 14.0f, 19.0f, 18.0f, 14.0f, 16.0f, 27.0f, 7.0f, 9.0f, 15.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 45.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 23.0f, 48.0f, 31.0f, 26.0f, 38.0f, 39.0f, 36.0f, 36.0f, 19.0f, 42.0f, 30.0f, 34.0f, 36.0f, 33.0f, 15.0f, 60.0f, 5.0f, 1.0f, 1.0f, 1.0f, 52.0f, 58.0f, 25.0f, 31.0f, 4.3307333f, 83.0f, 30.0f, 5.3033047f, 4.0430512f, 10.0f, 13.0f, 11.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 4.3307333f, 4.3307333f, 1.0f, 4.465908f, 4.465908f, 1.0f, 4.0430512f, 4.0430512f, 1.0f, 2.944439f, 2.944439f, 1.0f, 4.0775375f, 4.0775375f, 1.0f, 2.8903718f, 2.8903718f, 20.0f, 14.0f, 19.0f, 18.0f, 14.0f, 16.0f, 27.0f, 7.0f, 9.0f, 15.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f
    };

    public static final int CEOS_FAVORITE_PROJECT_DUMMY_ID = 20000;
    public static final int SYNTHETIC_CATASTOPHY_DUMMY_ID = 20001;
    public static final int PRIVATE_INVESTOR_BEACH_DUMMY_ID = 20002;
    public static final int RESEARCH_GRANT_DUMMY_ID = 20003;
    public static final int GENERIC_DUMMY_ID = 20100;
    public static final int SYNTHETIC_CATASTROPHY_CARD_ID = 218;

    public static final Card GENERIC_DUMMY_CARD = new GenericDummyCard(AiConstants.GENERIC_DUMMY_ID);
    public static final Card RESEARCH_GRANT_DUMMY_CARD = new ResearchGrantDummy(AiConstants.RESEARCH_GRANT_DUMMY_ID);

    public static final List<MilestoneType> MILESTONE_TYPES = List.of(
            MilestoneType.DIVERSIFIER,
            MilestoneType.ENERGIZER,
            MilestoneType.FARMER,
            MilestoneType.LEGEND,
            MilestoneType.MAGNATE,
            MilestoneType.PLANNER,
            MilestoneType.SPACE_BARON,
            MilestoneType.TERRAFORMER,
            MilestoneType.TYCOON,
            MilestoneType.GARDENER
    );

    public static final List<AwardType> AWARD_TYPES = List.of(
            AwardType.CELEBRITY,
            AwardType.COLLECTOR,
            AwardType.GENERATOR,
            AwardType.INDUSTRIALIST,
            AwardType.PROJECT_MANAGER,
            AwardType.RESEARCHER
    );

    public static final Set<Class<?>> ACTIONS_WITHOUT_INPUT_PARAMS = Set.of(
            AquiferPumping.class,
            ArtificialJungle.class,
            AssetLiquidation.class,
            Birds.class,
            BuffedBirds.class,
            Penguins.class,
            BrainstormingSession.class,
            CaretakerContract.class,
            CircuitBoardFactory.class,
            CommunityGardens.class,
            DevelopedInfrastructure.class,
            Sawmill.class,
            InterplanetarySuperhighway.class,
            DevelopmentCenter.class,
            FarmersMarket.class,
            HydroElectricEnergy.class,
            IronWorks.class,
            MatterManufactoring.class,
            BusinessNetwork.class,
            SolarPunk.class,
            Steelworks.class,
            Tardigrades.class,
            ThinkTank.class,
            VolcanicPools.class,
            WaterImportFromEuropa.class,
            WoodBurningStoves.class,
            ProgressivePolicies.class,
            DroneAssistedConstruction.class,
            FibrousCompositeActionValidator.class,
            SoftwareStreamlining.class,
            CityCouncil.class,
            CommunityAfforestation.class,
            GasCooledReactors.class
    );

    public static final Set<CardAction> CARD_ACTIONS_WITH_PHASE_UPGRADE_EFFECT = Set.of(
            CardAction.UPDATE_PHASE_CARD,
            CardAction.SOFTWARE_STREAMLINING
    );

    public static final List<Class<?>> ALLCARDS_ORDER = List.of(
            AdaptationTechnology.class,
            AdvancedAlloys.class,
            AdvancedScreeningTechnology.class,
            AiCentral.class,
            AnaerobicMicroorganisms.class,
            AntiGravityTechnology.class,
            AquiferPumping.class,
            ArcticAlgae.class,
            ArtificialJungle.class,
            AssemblyLines.class,
            AssetLiquidation.class,
            Birds.class,
            BrainstormingSession.class,
            CaretakerContract.class,
            CircuitBoardFactory.class,
            CommunityGardens.class,
            CompostingFactory.class,
            ConservedBiome.class,
            Decomposers.class,
            DecomposingFungus.class,
            DevelopedInfrastructure.class,
            DevelopmentCenter.class,
            EarthCatapult.class,
            EcologicalZone.class,
            EnergySubsidies.class,
            ExtendedResources.class,
            ExtremeColdFungus.class,
            FarmersMarket.class,
            FarmingCoops.class,
            Fish.class,
            GhgProductionBacteria.class,
            GreenHouses.class,
            Herbivores.class,
            HydroElectricEnergy.class,
            InterplanetaryRelations.class,
            Interns.class,
            InterplanetaryConference.class,
            IronWorks.class,
            Livestock.class,
            MarsUniversity.class,
            MatterManufactoring.class,
            MediaGroup.class,
            NitriteReductingBacteria.class,
            OlympusConference.class,
            OptimalAerobraking.class,
            PhysicsComplex.class,
            PowerInfrastructure.class,
            RecycledDetritus.class,
            RedraftedContracts.class,
            RegolithEaters.class,
            ResearchOutpost.class,
            SmallAnimals.class,
            RestructuredResources.class,
            SolarPunk.class,
            StandardTechnology.class,
            Steelworks.class,
            SymbioticFungus.class,
            Tardigrades.class,
            ThinkTank.class,
            UnitedPlanetaryAlliance.class,
            ViralEnhancers.class,
            VolcanicPools.class,
            WaterImportFromEuropa.class,
            WoodBurningStoves.class,
            AdvancedEcosystems.class,
            ArtificialLake.class,
            AtmosphereFiltering.class,
            BreathingFilters.class,
            BribedComittee.class,
            BusinessContracts.class,
            CeosFavoriteProject.class,
            ColonizerTrainingCamp.class,
            Comet.class,
            ConvoyFromEuropa.class,
            Crater.class,
            DeimosDown.class,
            GiantIceAsteroid.class,
            IceAsteroid.class,
            IceCapMelting.class,
            ImportedHydrogen.class,
            ImportedNitrogen.class,
            InterstellarColonyShip.class,
            InventionContest.class,
            InvestmentLoan.class,
            LagrangeObservatory.class,
            LakeMariners.class,
            LargeConvoy.class,
            LavaFlows.class,
            LocalHeatTrapping.class,
            Mangrove.class,
            NitrogenRichAsteroid.class,
            PermafrostExtraction.class,
            PhobosFalls.class,
            Plantation.class,
            ReleaseInertGases.class,
            Research.class,
            SpecialDesign.class,
            SubterraneanReservoir.class,
            TechnologyDemonstration.class,
            TerraformingGanymede.class,
            TowingAComet.class,
            WorkCrews.class,
            AcquiredCompany.class,
            AdaptedLichen.class,
            AeratedMagma.class,
            AirborneRadiation.class,
            Algae.class,
            Archaebacteria.class,
            ArtificialPhotosynthesis.class,
            AsteroidMining.class,
            AsteroidMiningConsortium.class,
            Astrofarm.class,
            AtmosphericInsulators.class,
            AutomatedFactories.class,
            BalancedPortfolios.class,
            BeamFromThoriumAsteroid.class,
            BiomassCombustors.class,
            BiothermalPower.class,
            Blueprints.class,
            BuildingIndustries.class,
            Bushes.class,
            CallistoPenalMines.class,
            Cartel.class,
            CoalImports.class,
            CommercialDistrict.class,
            DeepWellHeating.class,
            DesignedMicroorganisms.class,
            DiversifiedInterests.class,
            DustyQuarry.class,
            EconomicGrowth.class,
            EnergyStorage.class,
            EosChasmaNationalPark.class,
            Farming.class,
            FoodFactory.class,
            FuelFactory.class,
            FueledGenerators.class,
            FusionPower.class,
            GanymedeShipyard.class,
            GeneRepair.class,
            GeothermalPower.class,
            GiantSpaceMirror.class,
            Grass.class,
            GreatDam.class,
            GreatEscarpmentConsortium.class,
            Heather.class,
            ImmigrationShuttles.class,
            ImportOfAdvancedGHG.class,
            ImportedGHG.class,
            IndustrialCenter.class,
            IndustrialFarming.class,
            IndustrialMicrobes.class,
            Insects.class,
            IoMiningIndustries.class,
            KelpFarming.class,
            Lichen.class,
            LightningHarvest.class,
            LowAtmoShields.class,
            LunarBeam.class,
            MassConverter.class,
            MedicalLab.class,
            MethaneFromTitan.class,
            MicroMills.class,
            Microprocessors.class,
            Mine.class,
            MirandaResort.class,
            MoholeArea.class,
            Monocultures.class,
            Moss.class,
            NaturalPreserve.class,
            NewPortfolios.class,
            NitropholicMoss.class,
            NoctisFarming.class,
            NuclearPlants.class,
            PowerGrid.class,
            PowerPlant.class,
            PowerSupplyConsortium.class,
            ProtectedValley.class,
            QuantumExtractor.class,
            RadSuits.class,
            SatelliteFarms.class,
            Satellites.class,
            SlashAndBurnAgriculture.class,
            Smelting.class,
            SoilWarming.class,
            SolarPower.class,
            SolarTrapping.class,
            Soletta.class,
            SpaceHeater.class,
            SpaceStation.class,
            Sponsors.class,
            StripMine.class,
            SurfaceMines.class,
            TectonicStressPower.class,
            TitaniumMine.class,
            TallStation.class,
            TradingPost.class,
            TrappedHeat.class,
            Trees.class,
            TropicalResort.class,
            TundraFarming.class,
            UndergroundCity.class,
            UnderseaVents.class,
            VentureCapitalism.class,
            VestaShipyard.class,
            WavePower.class,
            Windmills.class,
            Worms.class,
            Zeppelins.class,
            AssortedEnterprises.class,
            CommercialImports.class,
            DiverseHabitats.class,
            Laboratories.class,
            MatterGenerator.class,
            ProcessedMetals.class,
            ProcessingPlant.class,
            ProgressivePolicies.class,
            FilterFeeders.class,
            SyntheticCatastrophe.class,
            SelfReplicatingBacteria.class,
            CommunicationsStreamlining.class,
            DroneAssistedConstruction.class,
            ExperimentalTechnology.class,
            ImpactAnalysis.class,
            HohmannTransferShipping.class,
            FibrousCompositeMaterial.class,
            SoftwareStreamlining.class,
            VirtualEmployeeDevelopment.class,
            VolcanicSoil.class,
            BiomedicalImports.class,
            CryogenicShipment.class,
            Exosuits.class,
            ImportedConstructionCrews.class,
            OreLeaching.class,
            PrivateInvestorBeach.class,
            TopographicMapping.class,
            ThreeDPrinting.class,
            Biofoundries.class,
            BlastFurnaces.class,
            Dandelions.class,
            ElectricArcFurnaces.class,
            LocalMarket.class,
            ManufacturingHub.class,
            HeatReflectiveGlass.class,
            HematiteMining.class,
            HydroponicGardens.class,
            IlmeniteDeposits.class,
            IndustrialComplex.class,
            MartianMuseum.class,
            Metallurgy.class,
            AwardWinningReflectorMaterial.class,
            PerfluorocarbonProduction.class,
            MagneticFieldGenerator.class,
            PoliticalInfluence.class,
            BiologicalFactories.class,
            NuclearDetonationSite.class,
            Warehouses.class,
            BacterialAggregates.class,
            CityCouncil.class,
            CommunityAfforestation.class,
            OrbitalOutpost.class,
            GasCooledReactors.class,
            ResearchGrant.class,
            Zoos.class,
            InnovativeTechnologiesAward.class,
            MartianStudiesScholarship.class,
            GeneticallyModifiedVegetables.class,
            GlacialEvaporation.class,
            Tourism.class
    );

    public static final List<Class<?>> RED_CARD_CLASSES = List.of(
            AdvancedEcosystems.class,
            ArtificialLake.class,
            AssortedEnterprises.class,
            AtmosphereFiltering.class,
            BreathingFilters.class,
            BribedComittee.class,
            BusinessContracts.class,
            CeosFavoriteProject.class,
            ColonizerTrainingCamp.class,
            Comet.class,
            ConvoyFromEuropa.class,
            Crater.class,
            DeimosDown.class,
            GiantIceAsteroid.class,
            IceAsteroid.class,
            IceCapMelting.class,
            ImportedHydrogen.class,
            ImportedNitrogen.class,
            InterstellarColonyShip.class,
            InventionContest.class,
            InvestmentLoan.class,
            LagrangeObservatory.class,
            LakeMariners.class,
            LargeConvoy.class,
            LavaFlows.class,
            LocalHeatTrapping.class,
            Mangrove.class,
            NitrogenRichAsteroid.class,
            PermafrostExtraction.class,
            PhobosFalls.class,
            Plantation.class,
            ReleaseInertGases.class,
            Research.class,
            SpecialDesign.class,
            SubterraneanReservoir.class,
            TechnologyDemonstration.class,
            TerraformingGanymede.class,
            TowingAComet.class,
            WorkCrews.class,
            BiomedicalImports.class,
            CryogenicShipment.class,
            Exosuits.class,
            ImportedConstructionCrews.class,
            InnovativeTechnologiesAward.class,
            MartianStudiesScholarship.class,
            OreLeaching.class,
            PrivateInvestorBeach.class,
            TopographicMapping.class,
            SyntheticCatastrophe.class
    );

    public static final List<Class<?>> BLUE_CARDS_WITH_SINGLE_FLAG = List.of(
            AdaptationTechnology.class,
            AdvancedAlloys.class,
            AdvancedScreeningTechnology.class,
            AiCentral.class,
            AnaerobicMicroorganisms.class,
            AntiGravityTechnology.class,
            AquiferPumping.class,
            ArcticAlgae.class,
            ArtificialJungle.class,
            AssemblyLines.class,
            AssetLiquidation.class,
            Birds.class,
            BrainstormingSession.class,
            CaretakerContract.class,
            CircuitBoardFactory.class,
            CommunityGardens.class,
            CompostingFactory.class,
            ConservedBiome.class,
            Decomposers.class,
            DecomposingFungus.class,
            DevelopedInfrastructure.class,
            DevelopmentCenter.class,
            EarthCatapult.class,
            EcologicalZone.class,
            EnergySubsidies.class,
            ExtendedResources.class,
            ExtremeColdFungus.class,
            FarmersMarket.class,
            FarmingCoops.class,
            FilterFeeders.class,
            Fish.class,
            GhgProductionBacteria.class,
            GreenHouses.class,
            Herbivores.class,
            HydroElectricEnergy.class,
            InterplanetaryRelations.class,
            Interns.class,
            InterplanetaryConference.class,
            IronWorks.class,
            Livestock.class,
            MarsUniversity.class,
            MatterManufactoring.class,
            MediaGroup.class,
            NitriteReductingBacteria.class,
            OlympusConference.class,
            OptimalAerobraking.class,
            PhysicsComplex.class,
            PowerInfrastructure.class,
            RecycledDetritus.class,
            RedraftedContracts.class,
            RegolithEaters.class,
            ResearchOutpost.class,
            SmallAnimals.class,
            RestructuredResources.class,
            SolarPunk.class,
            StandardTechnology.class,
            Steelworks.class,
            SymbioticFungus.class,
            Tardigrades.class,
            ThinkTank.class,
            UnitedPlanetaryAlliance.class,
            ViralEnhancers.class,
            VolcanicPools.class,
            WaterImportFromEuropa.class,
            WoodBurningStoves.class,
            ImmigrationShuttles.class,
            IoMiningIndustries.class,
            BacterialAggregates.class,
            CityCouncil.class,
            CommunicationsStreamlining.class,
            CommunityAfforestation.class,
            DroneAssistedConstruction.class,
            ExperimentalTechnology.class,
            FibrousCompositeMaterial.class,
            GasCooledReactors.class,
            HohmannTransferShipping.class,
            ImpactAnalysis.class,
            OrbitalOutpost.class,
            ResearchGrant.class,
            SoftwareStreamlining.class,
            VirtualEmployeeDevelopment.class,
            VolcanicSoil.class,
            Zoos.class
    );

    public static final List<Class<?>> GREEN_CARDS_WITH_SINGLE_FLAG = List.of(
            AeratedMagma.class,
            UnderseaVents.class,
            ArtificialPhotosynthesis.class,
            MethaneFromTitan.class,
            Blueprints.class,
            CommercialImports.class,
            Farming.class,
            IndustrialFarming.class,
            KelpFarming.class,
            TundraFarming.class,
            HydroponicGardens.class,
            LowAtmoShields.class,
            NuclearPlants.class,
            PowerSupplyConsortium.class,
            ManufacturingHub.class,
            NewPortfolios.class,
            Dandelions.class
    );

    public static final List<CardAction> corpCardActions = List.of(
            CardAction.HELION_CORPORATION,
            CardAction.CELESTIOR_CORPORATION,
            CardAction.DEVTECHS_CORPORATION,
            CardAction.LAUNCH_STAR_CORPORATION,
            CardAction.THORGATE_CORPORATION,
            CardAction.TERACTOR_CORPORATION,
            CardAction.THARSIS_CORPORATION,
            CardAction.CREDICOR_CORPORATION,
            CardAction.ARCLIGHT_CORPORATION,
            CardAction.SULTIRA_CORPORATION,
            CardAction.PHOBOLOG_CORPORATION,
            CardAction.HYPERION_SYSTEMS_CORPORATION,
            CardAction.MINING_GUILD_CORPORATION,
            CardAction.EXOCORP_CORPORATION,
            CardAction.SATURN_SYSTEMS_CORPORATION,
            CardAction.APOLLO_CORPORATION,
            CardAction.ZETACELL_CORPORATION,
            CardAction.AUSTELLAR_CORPORATION,
            CardAction.ECOLINE_CORPORATION,
            CardAction.MODPRO_CORPORATION,
            CardAction.INVENTRIX_CORPORATION,
            CardAction.NEBU_LABS_CORPORATION,
            CardAction.MAY_NI_PRODUCTIONS_CORPORATION,
            CardAction.UNMI_CORPORATION,
            CardAction.INTERPLANETARY_CINEMATICS
    );

    //12
    public static final List<Class<?>> NO_PAYMENT_BLUE_ACTIONS = List.of(
            AiCentral.class,
            CityCouncil.class,
            CircuitBoardFactory.class,
            Birds.class,
            CommunityGardens.class,
            ModproCorporation.class,
            AdvancedScreeningTechnology.class,
            CelestiorCorporation.class,
            BrainstormingSession.class,
            DroneAssistedConstruction.class,
            HyperionSystemsCorporation.class,
            Tardigrades.class
    );

    //37
    public static final List<Class<?>> WITH_PAY_BLUE_ACTIONS = List.of(
            AquiferPumping.class,
            ArtificialJungle.class,
            AssetLiquidation.class,
            CaretakerContract.class,
            ConservedBiome.class,
            DecomposingFungus.class,
            DevelopedInfrastructure.class,
            DevelopmentCenter.class,
            ExtremeColdFungus.class,
            FarmersMarket.class,
            FarmingCoops.class,
            GhgProductionBacteria.class,
            GreenHouses.class,
            HydroElectricEnergy.class,
            IronWorks.class,
            MatterGenerator.class,
            MatterManufactoring.class,
            NitriteReductingBacteria.class,
            PowerInfrastructure.class,
            ProgressivePolicies.class,
            RedraftedContracts.class,
            RegolithEaters.class,
            SelfReplicatingBacteria.class,
            SolarPunk.class,
            Steelworks.class,
            SymbioticFungus.class,
            ThinkTank.class,
            VolcanicPools.class,
            WaterImportFromEuropa.class,
            WoodBurningStoves.class,
            CommunityAfforestation.class,
            ExperimentalTechnology.class,
            FibrousCompositeMaterial.class,
            GasCooledReactors.class,
            ResearchGrant.class,
            SoftwareStreamlining.class,
            VirtualEmployeeDevelopment.class
    );


    public static final Map<Class<?>, Integer> BLUE_CARDS_INDEX_BY_CLASS;
    public static final Map<Class<?>, Integer> ALL_CARDS_INDEX_BY_CLASS;
    public static final Map<Class<?>, Integer> RED_CARDS_INDEX_BY_CLASS;
    public static final Map<Class<?>, Integer> GREEN_CARDS_INDEX_BY_CLASS;
    public static final Map<Class<?>, Integer> NO_PAY_BLUE_ACTION_INDEX_BY_CLASS;
    public static final Map<Class<?>, Integer> WITH_PAY_BLUE_ACTION_INDEX_BY_CLASS;

    public static final Map<CardAction, Integer> CORP_INDEX =
            IntStream.range(0, corpCardActions.size())
                    .boxed()
                    .collect(Collectors.toMap(
                            corpCardActions::get,
                            i -> i
                    ));

    static {
        Map<Class<?>, Integer> allCardsMap = new HashMap<>(ALLCARDS_ORDER.size());
        for (int i = 0; i < ALLCARDS_ORDER.size(); i++) {
            allCardsMap.put(ALLCARDS_ORDER.get(i), i);
        }
        ALL_CARDS_INDEX_BY_CLASS = allCardsMap;

        Map<Class<?>, Integer> redCardsMap = new HashMap<>(RED_CARD_CLASSES.size());
        for (int i = 0; i < RED_CARD_CLASSES.size(); i++) {
            redCardsMap.put(RED_CARD_CLASSES.get(i), i);
        }
        RED_CARDS_INDEX_BY_CLASS = redCardsMap;

        Map<Class<?>, Integer> blueCardsMap = new HashMap<>(BLUE_CARDS_WITH_SINGLE_FLAG.size());
        for (int i = 0; i < BLUE_CARDS_WITH_SINGLE_FLAG.size(); i++) {
            blueCardsMap.put(BLUE_CARDS_WITH_SINGLE_FLAG.get(i), i);
        }
        BLUE_CARDS_INDEX_BY_CLASS = blueCardsMap;

        Map<Class<?>, Integer> greenCardsMap = new HashMap<>(GREEN_CARDS_WITH_SINGLE_FLAG.size());
        for (int i = 0; i < GREEN_CARDS_WITH_SINGLE_FLAG.size(); i++) {
            greenCardsMap.put(GREEN_CARDS_WITH_SINGLE_FLAG.get(i), i);
        }
        GREEN_CARDS_INDEX_BY_CLASS = greenCardsMap;

        Map<Class<?>, Integer> noChoiceActionMMap = new HashMap<>(NO_PAYMENT_BLUE_ACTIONS.size());
        for (int i = 0; i < NO_PAYMENT_BLUE_ACTIONS.size(); i++) {
            noChoiceActionMMap.put(NO_PAYMENT_BLUE_ACTIONS.get(i), i);
        }
        NO_PAY_BLUE_ACTION_INDEX_BY_CLASS = noChoiceActionMMap;

        Map<Class<?>, Integer> choiceActionMap = new HashMap<>(WITH_PAY_BLUE_ACTIONS.size());
        for (int i = 0; i < WITH_PAY_BLUE_ACTIONS.size(); i++) {
            choiceActionMap.put(WITH_PAY_BLUE_ACTIONS.get(i), i);
        }
        WITH_PAY_BLUE_ACTION_INDEX_BY_CLASS = choiceActionMap;
    }
}
