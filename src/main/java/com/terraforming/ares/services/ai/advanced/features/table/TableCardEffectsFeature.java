package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.green.CommunicationsStreamlining;
import com.terraforming.ares.cards.green.ImmigrationShuttles;
import com.terraforming.ares.cards.green.IoMiningIndustries;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Set;

public class TableCardEffectsFeature implements FeatureBlock {
    public static final List<String> FEATURE_NAMES = List.of(
            "AdaptationTechnology",
            "AdvancedAlloys",
            "AdvancedScreeningTechnology",
            "AiCentral",
            "AnaerobicMicroorganisms",
            "AntiGravityTechnology",
            "AquiferPumping",
            "ArcticAlgae",
            "ArtificialJungle",
            "AssemblyLines",
            "AssetLiquidation",
            "Birds",
            "BrainstormingSession",
            "CaretakerContract",
            "CircuitBoardFactory",
            "CommunityGardens",
            "CompostingFactory",
            "ConservedBiome",
            "Decomposers",
            "DecomposingFungus",
            "DevelopedInfrastructure",
            "DevelopmentCenter",
            "EarthCatapult",
            "EcologicalZone",
            "EnergySubsidies",
            "ExtendedResources",
            "ExtremeColdFungus",
            "FarmersMarket",
            "FarmingCoops",
            "Fish",
            "GhgProductionBacteria",
            "GreenHouses",
            "Herbivores",
            "HydroElectricEnergy",
            "InterplanetaryRelations",
            "Interns",
            "InterplanetaryConference",
            "IronWorks",
            "Livestock",
            "MarsUniversity",
            "MatterManufactoring",
            "MediaGroup",
            "NitriteReductingBacteria",
            "OlympusConference",
            "OptimalAerobraking",
            "PhysicsComplex",
            "PowerInfrastructure",
            "RecycledDetritus",
            "RedraftedContracts",
            "RegolithEaters",
            "ResearchOutpost",
            "SmallAnimals",
            "RestructuredResources",
            "SolarPunk",
            "StandardTechnology",
            "Steelworks",
            "SymbioticFungus",
            "Tardigrades",
            "ThinkTank",
            "UnitedPlanetaryAlliance",
            "ViralEnhancers",
            "VolcanicPools",
            "WaterImportFromEuropa",
            "WoodBurningStoves",
            "ImmigrationShuttles",
            "IoMiningIndustries",
            "BacterialAggregates",
            "CityCouncil",
            "CommunicationsStreamlining",
            "CommunityAfforestation",
            "DroneAssistedConstruction",
            "ExperimentalTechnology",
            "FibrousCompositeMaterial",
            "GasCooledReactors",
            "HohmannTransferShipping",
            "ImpactAnalysis",
            "OrbitalOutpost",
            "ResearchGrant",
            "SoftwareStreamlining",
            "VirtualEmployeeDevelopment",
            "VolcanicSoil",
            "Zoos"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int size() {
        return BLUE_CARDS_WITH_SINGLE_FLAG.size();//82
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Set<Class<?>> playedCardClasses = ctx.getPlayedCardClasses();

        for (Class<?> aClass : BLUE_CARDS_WITH_SINGLE_FLAG) {
            out.write(playedCardClasses.contains(aClass) ? 1 : 0);
        }
    }

    private final List<Class<?>> BLUE_CARDS_WITH_SINGLE_FLAG = List.of(
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

}
