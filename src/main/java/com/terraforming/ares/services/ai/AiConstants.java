package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.corporations.GenericDummyCard;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.validation.action.FibrousCompositeActionValidator;

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
            1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 4.1431346f, 4.0253515f, 4.060443f, 1.6557958f, 1.2625923f, 1.2268381f, 1.2705147f, 0.36468655f, 0.04477874f, 0.8572333f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 107.5f, 70.0f, 62.0f, 12.0f, 11.0f, 25.0f, 47.0f, 9.0f, 410.0f, 127.0f, 245.0f, 30.625f, 0.875f, 53.0f, 30.0f, 20.0f, 10.0f, 43.0f, 10.0f, 4.0f, 18.0f, 13.0f, 18.0f, 17.0f, 14.0f, 15.0f, 26.0f, 7.0f, 8.0f, 13.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 15.875f, 0.875f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 45.0f, 7.0f, 1.0f, 6.0f, 1.0f, 33.0f, 50.0f, 1.0f, 2.0f, 5.0f, 1.0f, 1.0f, 1.0f, 2.0f, 2.0f, 1.0f, 6.0f, 1.0f, 1.0f, 3.0f, 3.0f, 1.0f, 1.0f, 7.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 23.0f, 1.0f, 19.0f, 1.0f, 25.0f, 1.0f, 16.0f, 1.0f, 34.0f, 1.0f, 1.0f, 53.0f, 1.0f, 1.0f, 21.0f, 1.0f, 1.0f, 30.0f, 1.0f, 1.0f, 25.0f, 1.0f, 12.0f, 1.0f, 1.0f, 50.0f, 1.0f, 1.0f, 5.0f, 3.0f, 4.0f, 6.0f, 3.0f, 7.0f, 1.0f, 10.0f, 1.0f, 20.0f, 1.0f, 10.0f, 4.0f, 1.0f, 1.0f, 31.0f, 1.0f, 47.0f, 1.0f, 52.0f, 1.0f, 10.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 27.0f, 3.0f, 1.0f, 1.0f, 27.0f, 1.0f, 10.0f, 3.0f, 1.0f, 40.0f, 1.0f, 15.0f, 1.0f, 12.0f, 1.0f, 12.0f, 2.0f, 1.0f, 1.0f, 14.0f, 1.0f, 15.0f, 1.0f, 12.0f, 1.0f, 1.0f, 1.0f, 27.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 107.5f, 70.0f, 62.0f, 12.0f, 11.0f, 25.0f, 47.0f, 9.0f, 410.0f, 127.0f, 245.0f, 30.625f, 0.875f, 53.0f, 30.0f, 20.0f, 10.0f, 43.0f, 10.0f, 4.0f, 18.0f, 13.0f, 18.0f, 17.0f, 14.0f, 15.0f, 26.0f, 7.0f, 8.0f, 13.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 15.875f, 0.875f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 45.0f, 7.0f, 1.0f, 6.0f, 1.0f, 33.0f, 50.0f, 1.0f, 2.0f, 5.0f, 1.0f, 1.0f, 1.0f, 2.0f, 2.0f, 1.0f, 6.0f, 1.0f, 1.0f, 3.0f, 3.0f, 1.0f, 1.0f, 7.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 23.0f, 1.0f, 19.0f, 1.0f, 25.0f, 1.0f, 16.0f, 1.0f, 34.0f, 1.0f, 1.0f, 53.0f, 1.0f, 1.0f, 21.0f, 1.0f, 1.0f, 30.0f, 1.0f, 1.0f, 25.0f, 1.0f, 12.0f, 1.0f, 1.0f, 50.0f, 1.0f, 1.0f, 5.0f, 3.0f, 4.0f, 6.0f, 3.0f, 7.0f, 1.0f, 10.0f, 1.0f, 20.0f, 1.0f, 10.0f, 4.0f, 1.0f, 1.0f, 31.0f, 1.0f, 47.0f, 1.0f, 52.0f, 1.0f, 10.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 27.0f, 3.0f, 1.0f, 1.0f, 27.0f, 1.0f, 10.0f, 3.0f, 1.0f, 40.0f, 1.0f, 15.0f, 1.0f, 12.0f, 1.0f, 12.0f, 2.0f, 1.0f, 1.0f, 14.0f, 1.0f, 15.0f, 1.0f, 12.0f, 1.0f, 1.0f, 1.0f, 27.0f
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
