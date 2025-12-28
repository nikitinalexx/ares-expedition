package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.corporations.CelestiorCorporation;
import com.terraforming.ares.cards.corporations.HyperionSystemsCorporation;
import com.terraforming.ares.cards.corporations.ModproCorporation;

import java.util.Set;

public final class EncoderConstants {
    private EncoderConstants() {

    }

    public static final Set<Class<?>> ASSEMBLY_LINES_EASY_CARDS = Set.of(
            CelestiorCorporation.class,
            HyperionSystemsCorporation.class,
            ModproCorporation.class,
            AdvancedScreeningTechnology.class,
            AiCentral.class,
            AssetLiquidation.class,
            Birds.class,
            BrainstormingSession.class,
            CircuitBoardFactory.class,
            CommunityGardens.class,
            ConservedBiome.class,
            ExtremeColdFungus.class,
            FarmersMarket.class,
            GhgProductionBacteria.class,
            HydroElectricEnergy.class,
            MatterGenerator.class,
            MatterManufactoring.class,
            NitriteReductingBacteria.class,
            RedraftedContracts.class,
            RegolithEaters.class,
            SelfReplicatingBacteria.class,
            SymbioticFungus.class,
            Tardigrades.class,
            ThinkTank.class,
            CityCouncil.class,
            DroneAssistedConstruction.class,
            FibrousCompositeMaterial.class,
            VirtualEmployeeDevelopment.class
    );
}
