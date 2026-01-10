package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.corporations.*;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EffectsSingleFlagFeature implements FeatureBlock {
    public static final List<String> FEATURE_NAMES = List.of(
            "AdvancedScreeningTechnology",
            "AntiGravityTechnology",
            "ArtificialJungle",
            "AssetLiquidation",
            "BrainstormingSession",
            "CaretakerContract",
            "DevelopmentCenter",
            "FarmersMarket",
            "FarmingCoops",
            "GreenHouses",
            "HydroElectricEnergy",
            "MarsUniversity",
            "MatterManufactoring",
            "OptimalAerobraking",
            "PowerInfrastructure",
            "RedraftedContracts",
            "RestructuredResources",
            "StandardTechnology",
            "ThinkTank",
            "ExperimentalTechnology",
            "OrbitalOutpost",
            "SoftwareStreamlining",
            "VirtualEmployeeDevelopment"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int size() {
        return BLUE_CARDS_WITH_SINGLE_FLAG.size();//23
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Set<Class<?>> playedCardClasses = ctx.getPlayedCardClasses();

        for (Class<?> aClass : BLUE_CARDS_WITH_SINGLE_FLAG) {
            out.write(playedCardClasses.contains(aClass) ? 1 : 0);
        }
    }

    private final List<Class<?>> BLUE_CARDS_WITH_SINGLE_FLAG = List.of(
            AdvancedScreeningTechnology.class,
            AntiGravityTechnology.class,
            ArtificialJungle.class,
            AssetLiquidation.class,
            BrainstormingSession.class,
            CaretakerContract.class,
            DevelopmentCenter.class,
            FarmersMarket.class,
            FarmingCoops.class,
            GreenHouses.class,
            HydroElectricEnergy.class,
            MarsUniversity.class,
            MatterManufactoring.class,
            OptimalAerobraking.class,
            PowerInfrastructure.class,
            RedraftedContracts.class,
            RestructuredResources.class,
            StandardTechnology.class,
            ThinkTank.class,
            ExperimentalTechnology.class,
            OrbitalOutpost.class,
            SoftwareStreamlining.class,
            VirtualEmployeeDevelopment.class
    );

}
