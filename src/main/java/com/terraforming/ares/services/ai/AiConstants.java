package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.corporations.GenericDummyCard;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.services.ai.turnProcessors.network2.buildParams.InputRequirementType;
import com.terraforming.ares.validation.action.FibrousCompositeActionValidator;

import java.util.List;
import java.util.Set;

public final class AiConstants {
    private AiConstants() {

    }

    public static final int WP_DENOMINATOR_FOR_FRONTIER = 6;
    public static final int BACTERIAL_AGGREGATES_COUNT_FOR_MICROBE_PUT = 4;
    public static final int TABLE_VECTOR_SIZE = 436;
    public static final int HAND_VECTOR_SIZE = 633;

    public static final int TOTAL_SIZE = TABLE_VECTOR_SIZE + HAND_VECTOR_SIZE;


    public static final float[] NORMALIZATION_VECTOR = new float[] {
            1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 4.1108737f, 4.189655f, 4.0943446f, 1.7181798f, 1.1968575f, 1.1583481f, 1.2360117f, 0.36468655f, 0.04477874f, 0.9645823f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 128.0f, 75.0f, 63.0f, 12.0f, 12.0f, 25.0f, 49.0f, 9.0f, 423.0f, 1.0f, 1.0f, 91.0f, 225.0f, 1.0f, 1.0f, 58.0f, 34.0f, 1.0f, 50.0f, 9.0f, 4.0f, 19.0f, 14.0f, 19.0f, 17.0f, 14.0f, 14.0f, 23.0f, 6.0f, 10.0f, 12.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 46.0f, 7.0f, 1.0f, 6.0f, 1.0f, 33.0f, 48.0f, 1.0f, 2.0f, 5.0f, 1.0f, 1.0f, 1.0f, 2.0f, 2.0f, 1.0f, 6.0f, 1.0f, 1.0f, 3.0f, 3.0f, 1.0f, 1.0f, 7.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 13.0f, 1.0f, 25.0f, 1.0f, 29.0f, 1.0f, 13.0f, 1.0f, 40.0f, 1.0f, 1.0f, 45.0f, 1.0f, 1.0f, 29.0f, 1.0f, 1.0f, 39.0f, 1.0f, 1.0f, 36.0f, 1.0f, 23.0f, 1.0f, 1.0f, 47.0f, 1.0f, 1.0f, 5.0f, 3.0f, 4.0f, 6.0f, 3.0f, 7.0f, 1.0f, 10.0f, 1.0f, 18.0f, 1.0f, 10.0f, 4.0f, 1.0f, 1.0f, 25.0f, 1.0f, 34.0f, 1.0f, 48.0f, 1.0f, 12.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 34.0f, 3.0f, 1.0f, 1.0f, 25.0f, 1.0f, 10.0f, 3.0f, 1.0f, 40.0f, 1.0f, 15.0f, 1.0f, 12.0f, 1.0f, 12.0f, 2.0f, 1.0f, 1.0f, 14.0f, 1.0f, 16.0f, 1.0f, 12.0f, 1.0f, 1.0f, 1.0f, 33.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 128.0f, 75.0f, 63.0f, 12.0f, 12.0f, 25.0f, 49.0f, 9.0f, 423.0f, 1.0f, 1.0f, 91.0f, 225.0f, 1.0f, 1.0f, 58.0f, 34.0f, 1.0f, 50.0f, 9.0f, 4.0f, 19.0f, 14.0f, 19.0f, 17.0f, 14.0f, 14.0f, 23.0f, 6.0f, 10.0f, 12.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 46.0f, 7.0f, 1.0f, 6.0f, 1.0f, 33.0f, 48.0f, 1.0f, 2.0f, 5.0f, 1.0f, 1.0f, 1.0f, 2.0f, 2.0f, 1.0f, 6.0f, 1.0f, 1.0f, 3.0f, 3.0f, 1.0f, 1.0f, 7.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 13.0f, 1.0f, 25.0f, 1.0f, 29.0f, 1.0f, 13.0f, 1.0f, 40.0f, 1.0f, 1.0f, 45.0f, 1.0f, 1.0f, 29.0f, 1.0f, 1.0f, 39.0f, 1.0f, 1.0f, 36.0f, 1.0f, 23.0f, 1.0f, 1.0f, 47.0f, 1.0f, 1.0f, 5.0f, 3.0f, 4.0f, 6.0f, 3.0f, 7.0f, 1.0f, 10.0f, 1.0f, 18.0f, 1.0f, 10.0f, 4.0f, 1.0f, 1.0f, 25.0f, 1.0f, 34.0f, 1.0f, 48.0f, 1.0f, 12.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 34.0f, 3.0f, 1.0f, 1.0f, 25.0f, 1.0f, 10.0f, 3.0f, 1.0f, 40.0f, 1.0f, 15.0f, 1.0f, 12.0f, 1.0f, 12.0f, 2.0f, 1.0f, 1.0f, 14.0f, 1.0f, 16.0f, 1.0f, 12.0f, 1.0f, 1.0f, 1.0f, 33.0f
    };

    public static final int CEOS_FAVORITE_PROJECT_DUMMY_ID = 20000;
    public static final int SYNTHETIC_CATASTOPHY_DUMMY_ID = 20001;
    public static final int PRIVATE_INVESTOR_BEACH_DUMMY_ID = 20002;
    public static final int RESEARCH_GRANT_DUMMY_ID = 20003;
    public static final int GENERIC_DUMMY_ID = 20100;

    public static final Card GENERIC_DUMMY_CARD = new GenericDummyCard(AiConstants.GENERIC_DUMMY_ID);
    public static final Card RESEARCH_GRANT_DUMMY_CARD = new ResearchGrantDummy(AiConstants.RESEARCH_GRANT_DUMMY_ID);

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
