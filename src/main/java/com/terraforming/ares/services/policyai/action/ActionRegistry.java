package com.terraforming.ares.services.policyai.action;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.cards.corporations.CelestiorCorporation;
import com.terraforming.ares.cards.corporations.HyperionSystemsCorporation;
import com.terraforming.ares.cards.corporations.ModproCorporation;
import com.terraforming.ares.cards.green.*;
import com.terraforming.ares.cards.red.*;
import com.terraforming.ares.services.ai.AiConstants;

import java.util.HashMap;
import java.util.Map;

public final class ActionRegistry {

    public static final int CARD_COUNT = 268;
    public static final int TARGET_ANIMAL_COUNT = 9;
    public static final int TARGET_MICROBE_COUNT = 9;
    public static final int TARGET_SCIENCE_COUNT = 2;
    public static final int RED_CARD_TARGET_COUNT = 48;
    public static final int TARGET_TAKE_PLANT_COUNT = 1;
    public static final int TARGET_SPEC_GREEN_CARD_INCOME_COUNT  = AiConstants.GREEN_CARDS_WITH_SINGLE_FLAG.size();
    /**
     * This is a separate head. Not part of regular target head.
     */
    public static final int TARGET_CORPORATIONS_COUNT = 25;
    public static final int SMALL_CHOICE_PHASE_UPGRADE_COUNT = 10;
    public static final int SMALL_CHOICE_TAG_COUNT = 10;
    public static final int SMALL_ACTION_COUNT_1_4_COUNT = 4;
    public static final int SMALL_PAY_FOR_THE_BUILD_COUNT = 4;
    public static final int SMALL_ACTION_STANDARD_PROJECT_COUNT = 3;
    public static final int SMALL_ACTION_HEAT_PLANTS_CONVERSION_COUNT = 2;

    public static final int BUILD_START;
    public static final int SELL_DISCARD_START;
    public static final int TARGET_ANIMAL_START;
    public static final int TARGET_MICROBE_START;
    public static final int TARGET_SCIENCE_START;
    public static final int TARGET_RED_CARD_START;
    public static final int TARGET_TAKE_PLANT_START;
    public static final int TARGET_NO_PAY_BLUE_ACTION_START;
    public static final int TARGET_WITH_PAY_BLUE_ACTION_START;
    public static final int TARGET_CORPORATIONS_START;
    public static final int TARGET_MAX_MC_INCOME;
    public static final int TARGET_MAX_HEAT_INCOME;
    public static final int TARGET_MAX_PLANTS_INCOME;
    public static final int TARGET_MAX_CARDS_INCOME;
    public static final int TARGET_SPEC_GREEN_CARD_INCOME_START;

    public static final int SMALL_CHOICE_PHASE_UPGRADE_START;
    public static final int SMALL_CHOICE_TAG_START;
    public static final int SMALL_BIOMEDICAL_IMPORTS_OXYGEN_START;
    public static final int SMALL_BIOMEDICAL_IMPORTS_PHASE_START;
    public static final int SMALL_DECOMPOSERS_PICK_MICROBE;
    public static final int SMALL_DECOMPOSERS_PICK_CARD;
    public static final int SMALL_ACTION_TAKE_MICROBE_START;
    public static final int SMALL_ACTION_USE_MICROBE_START;
    public static final int SMALL_ACTION_COUNT_1_4_START;
    public static final int SMALL_PAY_FOR_THE_BUILD_START;
    public static final int SMALL_ACTION_STANDARD_PROJECT_START;
    public static final int SMALL_ACTION_HEAT_PLANTS_CONVERSION_START;

    // ---- META ----
    public static final int SYSTEM_ENTER_SELL_MODE;
    public static final int SYSTEM_ENTER_HELION_MODE;
    public static final int SYSTEM_HEAT_EXCHANGE;
    public static final int SYSTEM_CONFIRM_PASS;
    public static final int SYSTEM_UNMI_RT_FOR_6_MC;
    public static final int SYSTEM_TAKE_BONUS;

    public static final int TOTAL_ACTIONS;

    public static final Map<Class<?>, Integer> ANIMAL_TARGET_TO_OFFSET = new HashMap<>();
    public static final Map<Class<?>, Integer> MICROBE_TARGET_TO_OFFSET = new HashMap<>();
    public static final Map<Class<?>, Integer> SCIENCE_TARGET_TO_OFFSET = new HashMap<>();

    public static final Map<Class<?>, Integer> SYNTHETIC_CATSTROPHE_TARGET = new HashMap<>();

    public static Map<Class<?>, Integer> ACTION_CARD_TARGET;

    static {
        ANIMAL_TARGET_TO_OFFSET.put(Birds.class, 0);
        ANIMAL_TARGET_TO_OFFSET.put(BuffedBirds.class, 0);

        ANIMAL_TARGET_TO_OFFSET.put(FilterFeeders.class, 1);
        ANIMAL_TARGET_TO_OFFSET.put(Fish.class, 2);
        ANIMAL_TARGET_TO_OFFSET.put(Livestock.class, 3);
        ANIMAL_TARGET_TO_OFFSET.put(Zoos.class, 4);
        ANIMAL_TARGET_TO_OFFSET.put(EcologicalZone.class, 5);
        ANIMAL_TARGET_TO_OFFSET.put(Herbivores.class, 6);
        ANIMAL_TARGET_TO_OFFSET.put(SmallAnimals.class, 7);

        ANIMAL_TARGET_TO_OFFSET.put(ArclightCorporation.class, 8);
        ANIMAL_TARGET_TO_OFFSET.put(BuffedArclightCorporation.class, 8);

        MICROBE_TARGET_TO_OFFSET.put(AnaerobicMicroorganisms.class, 0);
        MICROBE_TARGET_TO_OFFSET.put(Decomposers.class, 1);
        MICROBE_TARGET_TO_OFFSET.put(DecomposingFungus.class, 2);
        MICROBE_TARGET_TO_OFFSET.put(GhgProductionBacteria.class, 3);
        MICROBE_TARGET_TO_OFFSET.put(BuffedGhgProductionBacteria.class, 3);
        MICROBE_TARGET_TO_OFFSET.put(NitriteReductingBacteria.class, 4);
        MICROBE_TARGET_TO_OFFSET.put(RegolithEaters.class, 5);
        MICROBE_TARGET_TO_OFFSET.put(BuffedRegolithEaters.class, 5);

        MICROBE_TARGET_TO_OFFSET.put(SelfReplicatingBacteria.class, 6);
        MICROBE_TARGET_TO_OFFSET.put(Tardigrades.class, 7);
        MICROBE_TARGET_TO_OFFSET.put(BacterialAggregates.class, 8);


        SCIENCE_TARGET_TO_OFFSET.put(PhysicsComplex.class, 0);
        SCIENCE_TARGET_TO_OFFSET.put(FibrousCompositeMaterial.class, 1);

        SYNTHETIC_CATSTROPHE_TARGET.put(AdvancedEcosystems.class, 0);
        SYNTHETIC_CATSTROPHE_TARGET.put(ArtificialLake.class, 1);
        SYNTHETIC_CATSTROPHE_TARGET.put(AssortedEnterprises.class, 2);
        SYNTHETIC_CATSTROPHE_TARGET.put(AtmosphereFiltering.class, 3);
        SYNTHETIC_CATSTROPHE_TARGET.put(BreathingFilters.class, 4);
        SYNTHETIC_CATSTROPHE_TARGET.put(BribedComittee.class, 5);
        SYNTHETIC_CATSTROPHE_TARGET.put(BusinessContracts.class, 6);
        SYNTHETIC_CATSTROPHE_TARGET.put(CeosFavoriteProject.class, 7);
        SYNTHETIC_CATSTROPHE_TARGET.put(ColonizerTrainingCamp.class, 8);
        SYNTHETIC_CATSTROPHE_TARGET.put(Comet.class, 9);
        SYNTHETIC_CATSTROPHE_TARGET.put(ConvoyFromEuropa.class, 10);
        SYNTHETIC_CATSTROPHE_TARGET.put(Crater.class, 11);
        SYNTHETIC_CATSTROPHE_TARGET.put(DeimosDown.class, 12);
        SYNTHETIC_CATSTROPHE_TARGET.put(GiantIceAsteroid.class, 13);
        SYNTHETIC_CATSTROPHE_TARGET.put(IceAsteroid.class, 14);
        SYNTHETIC_CATSTROPHE_TARGET.put(IceCapMelting.class, 15);
        SYNTHETIC_CATSTROPHE_TARGET.put(ImportedHydrogen.class, 16);
        SYNTHETIC_CATSTROPHE_TARGET.put(ImportedNitrogen.class, 17);
        SYNTHETIC_CATSTROPHE_TARGET.put(InterstellarColonyShip.class, 18);
        SYNTHETIC_CATSTROPHE_TARGET.put(InventionContest.class, 19);
        SYNTHETIC_CATSTROPHE_TARGET.put(InvestmentLoan.class, 20);
        SYNTHETIC_CATSTROPHE_TARGET.put(LagrangeObservatory.class, 21);
        SYNTHETIC_CATSTROPHE_TARGET.put(LakeMariners.class, 22);
        SYNTHETIC_CATSTROPHE_TARGET.put(LargeConvoy.class, 23);
        SYNTHETIC_CATSTROPHE_TARGET.put(LavaFlows.class, 24);
        SYNTHETIC_CATSTROPHE_TARGET.put(LocalHeatTrapping.class, 25);
        SYNTHETIC_CATSTROPHE_TARGET.put(Mangrove.class, 26);
        SYNTHETIC_CATSTROPHE_TARGET.put(NitrogenRichAsteroid.class, 27);
        SYNTHETIC_CATSTROPHE_TARGET.put(PermafrostExtraction.class, 28);
        SYNTHETIC_CATSTROPHE_TARGET.put(PhobosFalls.class, 29);
        SYNTHETIC_CATSTROPHE_TARGET.put(Plantation.class, 30);
        SYNTHETIC_CATSTROPHE_TARGET.put(ReleaseInertGases.class, 31);
        SYNTHETIC_CATSTROPHE_TARGET.put(Research.class, 32);
        SYNTHETIC_CATSTROPHE_TARGET.put(SpecialDesign.class, 33);
        SYNTHETIC_CATSTROPHE_TARGET.put(SubterraneanReservoir.class, 34);
        SYNTHETIC_CATSTROPHE_TARGET.put(TechnologyDemonstration.class, 35);
        SYNTHETIC_CATSTROPHE_TARGET.put(TerraformingGanymede.class, 36);
        SYNTHETIC_CATSTROPHE_TARGET.put(TowingAComet.class, 37);
        SYNTHETIC_CATSTROPHE_TARGET.put(WorkCrews.class, 38);
        SYNTHETIC_CATSTROPHE_TARGET.put(BiomedicalImports.class, 39);
        SYNTHETIC_CATSTROPHE_TARGET.put(CryogenicShipment.class, 40);
        SYNTHETIC_CATSTROPHE_TARGET.put(Exosuits.class, 41);
        SYNTHETIC_CATSTROPHE_TARGET.put(ImportedConstructionCrews.class, 42);
        SYNTHETIC_CATSTROPHE_TARGET.put(InnovativeTechnologiesAward.class, 43);
        SYNTHETIC_CATSTROPHE_TARGET.put(MartianStudiesScholarship.class, 44);
        SYNTHETIC_CATSTROPHE_TARGET.put(OreLeaching.class, 45);
        SYNTHETIC_CATSTROPHE_TARGET.put(PrivateInvestorBeach.class, 46);
        SYNTHETIC_CATSTROPHE_TARGET.put(TopographicMapping.class, 47);

        ACTION_CARD_TARGET =
                Map.ofEntries(
                        Map.entry(AiCentral.class, 0),
                        Map.entry(CityCouncil.class, 1),
                        Map.entry(CircuitBoardFactory.class, 2),
                        Map.entry(Birds.class, 3),
                        Map.entry(CommunityGardens.class, 4),
                        Map.entry(ModproCorporation.class, 5),
                        Map.entry(AdvancedScreeningTechnology.class, 6),
                        Map.entry(CelestiorCorporation.class, 7),
                        Map.entry(BrainstormingSession.class, 8),
                        Map.entry(DroneAssistedConstruction.class, 9),
                        Map.entry(HyperionSystemsCorporation.class, 10),
                        Map.entry(Tardigrades.class, 11),
                        Map.entry(AquiferPumping.class, 12),
                        Map.entry(ArtificialJungle.class, 13),
                        Map.entry(AssetLiquidation.class, 14),
                        Map.entry(CaretakerContract.class, 15),
                        Map.entry(ConservedBiome.class, 16),
                        Map.entry(DecomposingFungus.class, 17),
                        Map.entry(DevelopedInfrastructure.class, 18),
                        Map.entry(DevelopmentCenter.class, 19),
                        Map.entry(ExtremeColdFungus.class, 20),
                        Map.entry(FarmersMarket.class, 21),
                        Map.entry(FarmingCoops.class, 22),
                        Map.entry(GhgProductionBacteria.class, 23),
                        Map.entry(GreenHouses.class, 24),
                        Map.entry(HydroElectricEnergy.class, 25),
                        Map.entry(IronWorks.class, 26),
                        Map.entry(MatterGenerator.class, 27),
                        Map.entry(MatterManufactoring.class, 28),
                        Map.entry(NitriteReductingBacteria.class, 29),
                        Map.entry(PowerInfrastructure.class, 30),
                        Map.entry(ProgressivePolicies.class, 31),
                        Map.entry(RedraftedContracts.class, 32),
                        Map.entry(RegolithEaters.class, 33),
                        Map.entry(SelfReplicatingBacteria.class, 34),
                        Map.entry(SolarPunk.class, 35),
                        Map.entry(Steelworks.class, 36),
                        Map.entry(SymbioticFungus.class, 37),
                        Map.entry(ThinkTank.class, 38),
                        Map.entry(VolcanicPools.class, 39),
                        Map.entry(WaterImportFromEuropa.class, 40),
                        Map.entry(WoodBurningStoves.class, 41),
                        Map.entry(CommunityAfforestation.class, 42),
                        Map.entry(ExperimentalTechnology.class, 43),
                        Map.entry(FibrousCompositeMaterial.class, 44),
                        Map.entry(GasCooledReactors.class, 45),
                        Map.entry(ResearchGrant.class, 46),
                        Map.entry(SoftwareStreamlining.class, 47),
                        Map.entry(VirtualEmployeeDevelopment.class, 48)
                );

        int i = 0;

        BUILD_START = i;
        i += CARD_COUNT;

        SELL_DISCARD_START = i;
        i += CARD_COUNT;

        {//target block
            TARGET_ANIMAL_START = i;
            i += TARGET_ANIMAL_COUNT;

            TARGET_MICROBE_START = i;
            i += TARGET_MICROBE_COUNT;

            TARGET_SCIENCE_START = i;
            i += TARGET_SCIENCE_COUNT;

            TARGET_RED_CARD_START = i;
            i += RED_CARD_TARGET_COUNT;

            TARGET_TAKE_PLANT_START = i;
            i += TARGET_TAKE_PLANT_COUNT;

            TARGET_NO_PAY_BLUE_ACTION_START = i;
            i += AiConstants.NO_PAYMENT_BLUE_ACTIONS.size();

            TARGET_WITH_PAY_BLUE_ACTION_START = i;
            i += AiConstants.WITH_PAY_BLUE_ACTIONS.size();

            TARGET_CORPORATIONS_START = i;
            i += TARGET_CORPORATIONS_COUNT;

            TARGET_MAX_MC_INCOME = i++;
            TARGET_MAX_HEAT_INCOME = i++;
            TARGET_MAX_PLANTS_INCOME = i++;
            TARGET_MAX_CARDS_INCOME = i++;

            TARGET_SPEC_GREEN_CARD_INCOME_START = i;
            i += TARGET_SPEC_GREEN_CARD_INCOME_COUNT;
        }

        {//small choice block
            SMALL_CHOICE_PHASE_UPGRADE_START = i;
            i += SMALL_CHOICE_PHASE_UPGRADE_COUNT;

            SMALL_CHOICE_TAG_START = i;
            i += SMALL_CHOICE_TAG_COUNT;

            SMALL_BIOMEDICAL_IMPORTS_OXYGEN_START = i++;
            SMALL_BIOMEDICAL_IMPORTS_PHASE_START = i++;

            SMALL_DECOMPOSERS_PICK_MICROBE = i++;
            SMALL_DECOMPOSERS_PICK_CARD = i++;

            SMALL_ACTION_TAKE_MICROBE_START = i++;
            SMALL_ACTION_USE_MICROBE_START = i++;

            SMALL_ACTION_COUNT_1_4_START = i;
            i += SMALL_ACTION_COUNT_1_4_COUNT;

            SMALL_PAY_FOR_THE_BUILD_START = i;
            i += SMALL_PAY_FOR_THE_BUILD_COUNT;

            SMALL_ACTION_STANDARD_PROJECT_START = i;
            i += SMALL_ACTION_STANDARD_PROJECT_COUNT;

            SMALL_ACTION_HEAT_PLANTS_CONVERSION_START = i;
            i += SMALL_ACTION_HEAT_PLANTS_CONVERSION_COUNT;
        }

        SYSTEM_ENTER_SELL_MODE = i++;
        SYSTEM_ENTER_HELION_MODE = i++;
        SYSTEM_HEAT_EXCHANGE = i++;
        SYSTEM_CONFIRM_PASS = i++;
        SYSTEM_UNMI_RT_FOR_6_MC = i++;
        SYSTEM_TAKE_BONUS = i++;

        TOTAL_ACTIONS = i;
    }

}
