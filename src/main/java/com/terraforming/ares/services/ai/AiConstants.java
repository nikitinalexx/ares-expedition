package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.validation.action.FibrousCompositeActionValidator;

import java.util.Set;

public final class AiConstants {
    private AiConstants() {

    }

    public static final Set<Class<?>> ACTIONS_WITHOUT_INPUT_PARAMS = Set.of(
            AquiferPumping.class,
            ArtificialJungle.class,
            AssetLiquidation.class,
            Birds.class,
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
}
