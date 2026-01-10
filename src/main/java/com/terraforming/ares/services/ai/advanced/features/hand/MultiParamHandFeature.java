package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.green.*;
import com.terraforming.ares.cards.red.*;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.advanced.EncoderConstants;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultiParamHandFeature implements FeatureBlock {
    @Override
    public int size() {
        return 203;
    }

    public static final List<String> FEATURE_NAMES = List.of(

            // Award Winning Reflector
            "hand.award_winning_reflector.present",
            "hand.award_winning_reflector.milestone_achieved",

            // Tourism
            "hand.tourism.present",
            "hand.tourism.milestones_count",

            // Automated Factories / Tall Station
            "hand.af_or_ts.normalized_presence",
            "hand.af_or_ts.cheap_green_cards_count",

            // Energy Storage
            "hand.energy_storage.present",
            "hand.energy_storage.tr_normalized",

            // Local Heat Trapping
            "hand.local_heat_trapping.present",
            "hand.local_heat_trapping.heat_available",

            // Tropical Resort
            "hand.tropical_resort.present",
            "hand.tropical_resort.heat_available",

            // Building Industries
            "hand.building_industries.present",
            "hand.building_industries.heat_available",

            // Fuel Factory
            "hand.fuel_factory.present",
            "hand.fuel_factory.heat_available",

            // Trees
            "hand.trees.present",
            "hand.trees.temperature_progress",

            // Tundra Farming
            "hand.tundra_farming.present",
            "hand.tundra_farming.temperature_progress",

            // Science engine cards
            "hand.ai_central.present",
            "hand.anti_gravity.present",
            "played.ai_central.present",
            "played.anti_gravity.present",
            "hand.circuit_board_factory.present",
            "played.circuit_board_factory.present",
            "hand.city_council.present",
            "played.city_council.present",
            "hand.virtual_employee.present",
            "played.virtual_employee.present",

            "science.potential_card_draw",
            "science.real_card_draw",
            "science.engine_enabled",
            "science.extra_science_tags",
            "science.progress_to_5",
            "science.virtual_employee_progress",

            // Microbe / plant / animal synergy
            "hand.anaerobic_microorganisms.present",
            "played.anaerobic_microorganisms.present",
            "hand.decomposers.present",
            "played.decomposers.present",
            "hand.am.trigger_tags_count",
            "hand.decomposers.trigger_tags_count",

            // Aquifer Pumping
            "hand.aquifer_pumping.present",
            "played.aquifer_pumping.present",
            "aquifer.oceans_left_ratio",
            "aquifer.steel_penalty",

            // Volcanic Pools
            "hand.volcanic_pools.present",
            "played.volcanic_pools.present",
            "volcanic_pools.oceans_left_ratio",
            "volcanic_pools.energy_required",

            // Community Afforestation
            "hand.community_afforestation.present",
            "played.community_afforestation.present",
            "community_afforestation.cost_modifier",

            // Experimental Technology
            "hand.experimental_technology.present",
            "played.experimental_technology.present",
            "experimental_technology.phase_cards",

            // Fibrous Composite Material
            "hand.fibrous_composite_material.present",
            "played.fibrous_composite_material.present",
            "fibrous_composite_material.phase_cards",

            // Volcanic Soil
            "hand.volcanic_soil.present",
            "played.volcanic_soil.present",
            "volcanic_soil.temperature_left_ratio",
            "volcanic_soil.oxygen_left_ratio",

            // Fish
            "hand.fish.present",
            "played.fish.present",
            "fish.oceans_left_ratio",
            "fish.temperature_progress",

            // Herbivores
            "hand.herbivores.present",
            "played.herbivores.present",
            "herbivores.triggers_left",
            "herbivores.triggers_left_played",
            "herbivores.ocean_progress",

            // Assembly Lines
            "hand.assembly_lines.present",
            "played.assembly_lines.present",
            "assembly_lines.easy_cards_count",
            "assembly_lines.active_cards_in_hand",

            // Caretaker Contract
            "hand.caretaker_contract.present",
            "played.caretaker_contract.present",
            "caretaker_contract.temperature_progress",

            // Heat economy
            "heat.useful_income_log",
            "heat.potential_capacity_log",

            // Green Houses
            "hand.green_houses.present",
            "played.green_houses.present",
            "green_houses.temperature_progress",

            // Innovative Technologies Award
            "hand.innovative_technologies_award.present",
            "innovative_technologies_award.temperature_progress",

            // Martian Studies Scholarship
            "hand.martian_studies_scholarship.present",

            // Imported Construction Crews
            "hand.imported_construction_crews.present",
            "imported_construction_crews.phase_cards",

            // GHG / Regolith / Nitrite
            "hand.ghg.present",
            "played.ghg.present",
            "hand.regolith_eaters.present",
            "played.regolith_eaters.present",
            "hand.nitrite_reducting.present",
            "played.nitrite_reducting.present",

            "nitrite.oceans_left_ratio",
            "regolith.oxygen_left_ratio",
            "ghg.temperature_left_ratio",

            // Self Replicating Bacteria
            "hand.self_replicating_bacteria.present",
            "played.self_replicating_bacteria.present",
            "self_replicating_bacteria.resource_count",
            "self_replicating_bacteria.max_card_cost_synergy",

            // LIVESTOCK
            "livestock_hand",
            "livestock_played",
            "livestock_temperature_left_ratio",
            "livestock_oxygen_availability_progress",

            // SMALL ANIMALS
            "small_animals_hand",
            "small_animals_played",
            "small_animals_min_red_temp_availability",
            "small_animals_greenery_potential",
            "small_animals_greenery_potential_x_oxygen_left",

            // ZOOS
            "zoos_hand",
            "zoos_played",
            "zoos_animals_on_card",
            "zoos_unclaimed_milestones",

            // DEVELOPED INFRASTRUCTURE
            "developed_infrastructure_hand",
            "developed_infrastructure_played",
            "developed_infrastructure_temperature_left_ratio",
            "developed_infrastructure_blue_cards_penalty",

            // GAS COOLED REACTORS
            "gas_cooled_reactors_hand",
            "gas_cooled_reactors_played",
            "gas_cooled_reactors_temperature_left_ratio",
            "gas_cooled_reactors_remaining_power",

            // WATER IMPORT / IO MINING / TERRAFORMING GANYMEDE
            "water_import_hand",
            "water_import_played",
            "io_mining_hand",
            "io_mining_played",
            "water_import_oceans_left_ratio",
            "water_import_titanium_gap",
            "jupiter_tags_played_synergy",
            "jupiter_tags_hand_synergy",
            "terraforming_ganymede_played_jupiter_tags",

            // PROGRESSIVE POLICIES
            "progressive_policies_hand",
            "progressive_policies_played",
            "progressive_policies_oxygen_left_ratio",
            "progressive_policies_event_penalty",

            // IRON WORKS
            "iron_works_hand",
            "iron_works_played",
            "iron_works_oxygen_left_ratio",

            // STEEL WORKS
            "steel_works_hand",
            "steel_works_played",
            "steel_works_oxygen_left_ratio",

            // SOLAR PUNK
            "solar_punk_hand",
            "solar_punk_played",
            "solar_punk_oxygen_left_ratio",
            "solar_punk_titanium_gap",
            "solar_punk_income_log",

            // STANDARD TECHNOLOGY
            "standard_technology_hand",
            "standard_technology_played",
            "standard_technology_total_global_progress",
            "standard_technology_income_log",

            // BIRDS
            "birds_hand",
            "birds_played",
            "birds_oxygen_availability_progress",

            // TARDIGRADES
            "tardigrades_hand",
            "tardigrades_played",

            // PHYSICS COMPLEX
            "physics_complex_hand",
            "physics_complex_played",
            "physics_complex_temperature_left_ratio",
            "physics_complex_science_threshold_met",
            "physics_complex_science_progress",

            // PLANTATION
            "plantation_hand",
            "plantation_science_threshold_met",
            "plantation_science_progress",

            // MARS UNIVERSITY
            "mars_university_played",
            "mars_university_hand",
            "mars_university_hand_science_tags",
            "mars_university_hand_plant_tags",

            // MATTER GENERATOR
            "matter_generator_played",
            "matter_generator_hand",
            "matter_generator_hand_size",

            // REDRAFTED CONTRACTS / SOFTWARE STREAMLINING
            "redrafted_contracts_played",
            "redrafted_contracts_hand",
            "software_streamlining_played",
            "software_streamlining_hand",
            "redrafted_contracts_cycling_utility",
            "software_streamlining_cycling_utility",

            // ARTIFICIAL JUNGLE / WOOD BURNING STOVES
            "artificial_jungle_hand",
            "artificial_jungle_played",
            "wood_burning_stoves_temperature_left_ratio",
            "plant_synergy_has_plants",
            "plant_synergy_plants_log",
            "plant_synergy_has_plant_income",
            "plant_synergy_plant_income_log",
            "wood_burning_stoves_can_convert_plants",

            // INTERPLANETARY RELATIONS
            "interplanetary_relations_hand",
            "interplanetary_relations_played",
            "interplanetary_relations_played_cards_potential",

            // IMMIGRATION SHUTTLES
            "immigration_shuttles_hand",
            "immigration_shuttles_played",
            "immigration_shuttles_played_earth_tags",
            "immigration_shuttles_hand_earth_tags",
            "immigration_shuttles_earth_income",

            // ADVANCED ECOSYSTEMS
            "advanced_ecosystems_hand",
            "advanced_ecosystems_satisfied_ratio",

            // CRATER
            "crater_hand",
            "crater_event_progress",

            // NITROGEN RICH ASTEROID
            "nitrogen_rich_asteroid_hand",
            "nitrogen_rich_asteroid_plant_progress",

            // SYNTHETIC CATASTROPHE
            "synthetic_catastrophe_hand",
            "synthetic_catastrophe_red_cards_in_hand",

            // WORK CREWS
            "work_crews_hand",
            "work_crews_red_blue_cards_in_hand"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Map<CardAction, Long> handCardActions = ctx.getHandCardActions();
        Set<Class<?>> handCardClasses = ctx.getHandCardClasses();
        Map<CardAction, Long> playedCardActions = ctx.getPlayedCardActions();
        Set<Class<?>> playedCardClasses = ctx.getPlayedCardClasses();
        CardService cardService = ctx.getCardService();

        List<Card> hand = ctx.getHand();
        int[] handTagCounts = ctx.getHandTagCounts();

        MarsGame game = ctx.getGame();
        Player player = ctx.getPlayer();

        final float science = ctx.getPlayedTagToCount().getOrDefault(Tag.SCIENCE, 0L).floatValue();
        int plant = ctx.getPlayedTagToCount().getOrDefault(Tag.PLANT, 0L).intValue();
        int microbe = ctx.getPlayedTagToCount().getOrDefault(Tag.MICROBE, 0L).intValue();
        int animal = ctx.getPlayedTagToCount().getOrDefault(Tag.ANIMAL, 0L).intValue();
        final float earth = ctx.getPlayedTagToCount().getOrDefault(Tag.EARTH, 0L).floatValue();
        final float playedEnergy = ctx.getPlayedTagToCount().getOrDefault(Tag.ENERGY, 0L).floatValue();
        final float playedJupiterTagCount = ctx.getPlayedTagToCount().getOrDefault(Tag.JUPITER, 0L).floatValue();
        final float playedEvent = ctx.getPlayedTagToCount().getOrDefault(Tag.EVENT, 0L).floatValue();

        Planet planet = game.getPlanet();
        float oxygenLeftRatio = (float) planet.oxygenLeft() / planet.oxygenMax();
        float temperatureLeftRatio = (float) planet.temperatureLeft() / planet.temperatureMax();
        float oceansLeftRatio = (float) planet.oceansLeft() / planet.oceansMaxCount();

        {
            boolean awardWinning = handCardActions.containsKey(CardAction.AWARD_WINNING_REFLECTOR);
            out.write(awardWinning ? 1f : 0f);
            out.write(awardWinning && game.getMilestones().stream().anyMatch(milestone -> milestone.isAchieved(player)) ? 1f : 0f);
        }

        {
            boolean tourism = handCardActions.containsKey(CardAction.TOURISM);
            out.write(tourism ? 1f : 0f);
            out.write(tourism ? game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count() : 0f);
        }

        {
            boolean handAf = handCardActions.containsKey(CardAction.AUTOMATED_FACTORIES);
            boolean handTS = handCardActions.containsKey(CardAction.TALL_STATION);
            out.write(handAf || handTS ? (((handAf ? 1 : 0) + (handTS ? 1 : 0)) / 2.0F) : 0);
            out.write(handAf || handTS ? hand.stream().filter(card -> card.getColor() == CardColor.GREEN && card.getPrice() <= 9).count() : 0);
        }

        {
            boolean energyStorage = handCardActions.containsKey(CardAction.ENERGY_STORAGE);
            out.write(energyStorage ? 1f : 0f);
            out.write(energyStorage ? Math.min(1.0f, (player.getTerraformingRating() / 7f)) : 0f);
        }

        {//-3 heat, +4 plants, +2 animals or + 2 microbes
            boolean localHeatTrapping = handCardActions.containsKey(CardAction.LOCAL_HEAT_TRAPPING);
            out.write(localHeatTrapping ? 1f : 0f);
            out.write(localHeatTrapping ? (float) Math.min(1.0, player.getHeat() / 3f) : 0f);
        }

        {
            boolean tropicalResort = handCardClasses.contains(TropicalResort.class);//-5 heat
            out.write(tropicalResort ? 1f : 0f);
            out.write(tropicalResort ? (float) Math.min(1.0, player.getHeat() / 5f) : 0f);
        }

        {
            boolean buildingIndustries = handCardClasses.contains(BuildingIndustries.class);//-4 heat
            out.write(buildingIndustries ? 1f : 0f);
            out.write(buildingIndustries ? (float) Math.min(1.0, player.getHeat() / 4f) : 0f);
        }

        {
            boolean fuelFactory = handCardClasses.contains(FuelFactory.class);//-3 heat
            out.write(fuelFactory ? 1f : 0f);
            out.write(fuelFactory ? (float) Math.min(1.0, player.getHeat() / 3f) : 0f);
        }

        {
            boolean trees = handCardClasses.contains(Trees.class);
            out.write(trees ? 1f : 0f);
            out.write(trees ? ctx.getMinYellowTemperatureAvailabilityProgress() : 0f);
        }

        {
            boolean tundraFarming = handCardClasses.contains(TundraFarming.class);
            out.write(tundraFarming ? 1f : 0f);
            out.write(tundraFarming ? ctx.getMinYellowTemperatureAvailabilityProgress() : 0f);
        }


        {
            boolean handAiCentral = handCardClasses.contains(AiCentral.class);
            boolean handAntiGravity = handCardClasses.contains(AntiGravityTechnology.class);
            boolean playedAiCentral = playedCardClasses.contains(AiCentral.class);
            boolean playedAntiGravity = playedCardClasses.contains(AntiGravityTechnology.class);
            boolean handCircuitBoard = handCardClasses.contains(CircuitBoardFactory.class);
            boolean playedCircuitBoard = playedCardClasses.contains(CircuitBoardFactory.class);
            boolean handCityCouncil = handCardClasses.contains(CityCouncil.class);
            boolean playedCityCouncil = playedCardClasses.contains(CityCouncil.class);
            boolean handVirtualEmployee = handCardClasses.contains(VirtualEmployeeDevelopment.class);
            boolean playedVirtualEmployee = playedCardClasses.contains(VirtualEmployeeDevelopment.class);


            out.write(handAiCentral ? 1 : 0);
            out.write(handAntiGravity ? 1 : 0);

            out.write(playedAiCentral ? 1 : 0);
            out.write(playedAntiGravity ? 1 : 0);

            out.write(handCircuitBoard ? 1 : 0);
            out.write(playedCircuitBoard ? 1 : 0);

            out.write(handCityCouncil ? 1 : 0);
            out.write(playedCityCouncil ? 1 : 0);

            out.write(handVirtualEmployee ? 1 : 0);
            out.write(playedVirtualEmployee ? 1 : 0);

            int potentialGetCards = (handAiCentral ? 2 : 0) + (handCircuitBoard ? 1 : 0);
            int realGetCards = (playedAiCentral ? 2 : 0) + (playedCircuitBoard ? 1 : 0);

            if (handCityCouncil || playedCityCouncil) {
                int milestonesAchieved = (int) game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count();
                if (handCityCouncil) {
                    potentialGetCards += (1 + milestonesAchieved);
                }
                if (playedCityCouncil) {
                    realGetCards += (1 + milestonesAchieved);
                }
            }

            out.write(potentialGetCards);
            out.write(realGetCards);

            out.write((handAiCentral || handAntiGravity) && science >= 5 ? 1 : 0);
            out.write((handAiCentral || handAntiGravity) ? Math.max(0, handTagCounts[Tag.SCIENCE.ordinal()] - (handAiCentral ? 1 : 0) - (handAntiGravity ? 1 : 0)) : 0);
            out.write((handAiCentral || handAntiGravity) ? Math.min(1.0f, science / 5.f) : 0);
            out.write(handVirtualEmployee ? Math.min(1.0f, science / 3.f) : 0);
        }

        {
            boolean handAM = handCardClasses.contains(AnaerobicMicroorganisms.class);
            boolean playedAM = playedCardClasses.contains(AnaerobicMicroorganisms.class);
            boolean handDecomposers = handCardClasses.contains(Decomposers.class);
            boolean playedDecomposers = playedCardClasses.contains(Decomposers.class);

            out.write(handAM ? 1 : 0);
            out.write(playedAM ? 1 : 0);
            out.write(handDecomposers ? 1 : 0);
            out.write(playedDecomposers ? 1 : 0);

            out.write(handAM || playedAM ? handTagCounts[Tag.ANIMAL.ordinal()] + handTagCounts[Tag.PLANT.ordinal()] + handTagCounts[Tag.MICROBE.ordinal()] : 0);
            out.write(handDecomposers || playedDecomposers ? handTagCounts[Tag.ANIMAL.ordinal()] + handTagCounts[Tag.PLANT.ordinal()] + handTagCounts[Tag.MICROBE.ordinal()] : 0);
        }

        //OCEAN BLUE CARDS
        {//good example
            boolean handAquifer = handCardActions.containsKey(CardAction.AQUIFER_PUMPING);
            boolean playedAquifer = playedCardActions.containsKey(CardAction.AQUIFER_PUMPING);

            out.write(handAquifer ? 1 : 0);
            out.write(playedAquifer ? 1 : 0);

            out.write((handAquifer || playedAquifer) ? oceansLeftRatio : 0);
            out.write((handAquifer || playedAquifer) && !planet.isOceansMax() ? (10 - Math.min(5, player.getSteelIncome()) * 2) : 0);
        }

        {
            boolean handVolcanicPools = handCardClasses.contains(VolcanicPools.class);
            boolean playedVolcanicPools = playedCardClasses.contains(VolcanicPools.class);

            out.write(handVolcanicPools ? 1 : 0);
            out.write(playedVolcanicPools ? 1 : 0);

            out.write(playedVolcanicPools || handVolcanicPools ? oceansLeftRatio : 0);
            out.write((handVolcanicPools || playedVolcanicPools) && !planet.isOceansMax() ? Math.max(0, 12 - playedEnergy) : 0);
        }//50

        {
            boolean handCA = handCardClasses.contains(CommunityAfforestation.class);
            boolean playedCA = playedCardClasses.contains(CommunityAfforestation.class);

            out.write(handCA ? 1 : 0);
            out.write(playedCA ? 1 : 0);
            out.write(handCA || playedCA ? (14 - game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count() * 4) : 0f);
        }


        {
            boolean handAT = handCardActions.containsKey(CardAction.EXPERIMENTAL_TECHNOLOGY);
            boolean playedAT = playedCardActions.containsKey(CardAction.EXPERIMENTAL_TECHNOLOGY);
            out.write(handAT ? 1 : 0);
            out.write(playedAT ? 1 : 0);
            out.write((handAT || playedAT) ? player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count() : 0);
        }

        {
            boolean handFCM = handCardActions.containsKey(CardAction.FIBROUS_COMPOSITE_MATERIAL);
            boolean playedFCM = playedCardActions.containsKey(CardAction.FIBROUS_COMPOSITE_MATERIAL);
            out.write(handFCM ? 1 : 0);
            out.write(playedFCM ? 1 : 0);
            out.write((handFCM || playedFCM) ? player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count() : 0);
        }


        {//good example
            boolean handSoil = handCardActions.containsKey(CardAction.VOLCANIC_SOIL);
            boolean playedSoil = playedCardActions.containsKey(CardAction.VOLCANIC_SOIL);

            out.write(handSoil ? 1 : 0);
            out.write(playedSoil ? 1 : 0);

            out.write((handSoil || playedSoil) ? temperatureLeftRatio : 0);
            out.write((handSoil || playedSoil) ? oxygenLeftRatio : 0);
        }

        {//good example
            boolean handFish = handCardActions.containsKey(CardAction.FISH);
            boolean playedFish = playedCardActions.containsKey(CardAction.FISH);

            out.write(handFish ? 1 : 0);
            out.write(playedFish ? 1 : 0);

            out.write((playedFish || handFish) ? oceansLeftRatio : 0);
            out.write(handFish ? ctx.getMinRedTemperatureAvailabilityProgress() : 0);
        }

        {//good example
            boolean handHerbivores = handCardActions.containsKey(CardAction.HERBIVORES);
            boolean playedHerbivores = playedCardActions.containsKey(CardAction.HERBIVORES);

            out.write(handHerbivores ? 1 : 0);
            out.write(playedHerbivores ? 1 : 0);

            float triggersLeft = planet.oceansLeft() + planet.temperatureLeft() + planet.oxygenLeft();

            out.write(handHerbivores ? triggersLeft : 0);
            out.write(playedHerbivores ? triggersLeft : 0);
            out.write(handHerbivores ? Math.min(1, (float) planet.oceansBuilt() / 5) : 0);//availability progress
        }

        {//Assembly Lines
            boolean handAL = handCardClasses.contains(AssemblyLines.class);
            boolean playedAL = playedCardClasses.contains(AssemblyLines.class);

            out.write(handAL ? 1 : 0);
            out.write(playedAL ? 1 : 0);

            out.write(handAL || playedAL ? playedCardClasses.stream().filter(EncoderConstants.ASSEMBLY_LINES_EASY_CARDS::contains).count() : 0f);
            out.write(handAL || playedAL ? hand.stream().filter(Card::isActiveCard).count() : 0f);
        }


        {
            boolean handCaretakerContract = handCardClasses.contains(CaretakerContract.class);
            out.write(handCaretakerContract ? 1 : 0);
            out.write(playedCardClasses.contains(CaretakerContract.class) ? 1 : 0);

            out.write(handCaretakerContract ? ctx.getMinYellowTemperatureAvailabilityProgress() : 0);
        }


        {
            float heatIncome = (float) player.getHeatIncome();
            float heatCapacity = getHeatConsumeCapacityOnActions(playedCardActions);

            // сколько тепла реально можно потратить сейчас
            float usefulHeatIncome;
            if (!planet.isTemperatureMax()) {
                usefulHeatIncome = heatIncome;
            } else {
                usefulHeatIncome = Math.min(heatIncome, heatCapacity);
            }

            out.write((float) Math.log1p(usefulHeatIncome));

            // потенциальная capacity из руки (без infinite-хака)
            float potentialHeatCapacity = getHeatConsumeCapacityOnActions(handCardActions);

            out.write((float) Math.log1p(potentialHeatCapacity));

        }


        {
            boolean handGreenHouses = handCardActions.containsKey(CardAction.GREEN_HOUSES);
            out.write(handGreenHouses ? 1 : 0);
            out.write(playedCardActions.containsKey(CardAction.GREEN_HOUSES) ? 1 : 0);
            out.write(handGreenHouses ? ctx.getMinYellowTemperatureAvailabilityProgress() : 0);
        }

        {
            boolean handITA = handCardClasses.contains(InnovativeTechnologiesAward.class);
            out.write(handITA ? 1 : 0);
            out.write(handITA ? ctx.getMinYellowTemperatureAvailabilityProgress() : 0);
        }

        {
            out.write(handCardClasses.contains(MartianStudiesScholarship.class) ? 1 : 0);
        }

        {
            boolean handICC = handCardClasses.contains(ImportedConstructionCrews.class);
            out.write(handICC ? 1 : 0);
            out.write(handICC ? player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count() : 0);
        }

        boolean ghgHand = handCardActions.containsKey(CardAction.GHG_PRODUCTION);
        boolean ghgTable = playedCardActions.containsKey(CardAction.GHG_PRODUCTION);

        boolean regolithHand = handCardActions.containsKey(CardAction.REGOLITH_EATERS);
        boolean regolithTable = playedCardActions.containsKey(CardAction.REGOLITH_EATERS);

        boolean nitriteHand = handCardActions.containsKey(CardAction.NITRITE_REDUCTING);
        boolean nitriteTable = playedCardActions.containsKey(CardAction.NITRITE_REDUCTING);

        out.write(ghgHand ? 1 : 0);
        out.write(ghgTable ? 1 : 0);

        out.write(regolithHand ? 1 : 0);
        out.write(regolithTable ? 1 : 0);

        out.write(nitriteHand ? 1 : 0);
        out.write(nitriteTable ? 1 : 0);

        out.write(nitriteHand || nitriteTable ? oceansLeftRatio : 0f);
        out.write(regolithHand || regolithTable ? oxygenLeftRatio : 0f);
        out.write(ghgHand || ghgTable ? temperatureLeftRatio : 0f);

        {
            boolean handSelfRep = handCardActions.containsKey(CardAction.SELF_REPLICATING_BACTERIA);
            boolean playedSelfRep = playedCardActions.containsKey(CardAction.SELF_REPLICATING_BACTERIA);

            out.write(handSelfRep ? 1 : 0);
            out.write(playedSelfRep ? 1 : 0);//100

            out.write(playedSelfRep ? player.getCardResourcesCount().getOrDefault(SelfReplicatingBacteria.class, 0) : 0);

            float maxCostInHand = (float) hand.stream()
                    .filter(c -> !c.getClass().equals(SelfReplicatingBacteria.class))
                    .mapToInt(Card::getPrice)
                    .max()
                    .orElse(0);

            out.write((playedSelfRep && player.getCardResourcesCount()
                    .getOrDefault(SelfReplicatingBacteria.class, 0) >= 3)
                    ? Math.min(1.0f, maxCostInHand / 25.0f)
                    : 0);
        }


        {
            boolean handLivestock = handCardActions.containsKey(CardAction.LIVESTOCK);
            boolean playedLivestock = playedCardActions.containsKey(CardAction.LIVESTOCK);

            out.write(handLivestock ? 1 : 0);
            out.write(playedLivestock ? 1 : 0);

            out.write((playedLivestock || handLivestock) ? temperatureLeftRatio : 0);

            int currentOxygenLevel = planet.getCurrentLevel(GlobalParameter.OXYGEN);
            int targetOxygenLevel = getOxygenStepFromColor(ctx.isCanAmplifyOxygenOrTemperature() ? ParameterColor.R : ParameterColor.Y, false);

            float availabilityProgress = (currentOxygenLevel >= targetOxygenLevel) ? 1f : (float) currentOxygenLevel / targetOxygenLevel;
            out.write(handLivestock ? availabilityProgress : 0);
        }

        {
            boolean handSA = handCardActions.containsKey(CardAction.SMALL_ANIMALS);
            boolean playedSA = playedCardActions.containsKey(CardAction.SMALL_ANIMALS);

            out.write(handSA ? 1 : 0);
            out.write(playedSA ? 1 : 0);
            out.write(handSA ? ctx.getMinRedTemperatureAvailabilityProgress() : 0);

            float greeneryPotential = (float) (Math.log1p(player.getPlantsIncome()) + 0.5f * Math.log1p(player.getMcIncome() + player.getTerraformingRating()));
            out.write((handSA || playedSA) ? greeneryPotential : 0);
            out.write((handSA || playedSA) ? greeneryPotential * oxygenLeftRatio : 0);
        }

        {
            boolean handZoos = handCardActions.containsKey(CardAction.ZOOS);
            boolean playedZoos = playedCardActions.containsKey(CardAction.ZOOS);

            out.write(handZoos ? 1 : 0);
            out.write(playedZoos ? 1 : 0);

            out.write(playedZoos ? player.getCardResourcesCount().get(Zoos.class) : 0);
            out.write((handZoos || playedZoos) ? game.getMilestones().stream().filter(milestone -> !milestone.isAchieved()).count() : 0);
        }

        {
            boolean handDevelopedInfrastructure = handCardClasses.contains(DevelopedInfrastructure.class);
            boolean playedDevelopedInfrastructure = playedCardClasses.contains(DevelopedInfrastructure.class);

            out.write(handDevelopedInfrastructure ? 1 : 0);
            out.write(playedDevelopedInfrastructure ? 1 : 0);

            out.write((playedDevelopedInfrastructure || handDevelopedInfrastructure) ? temperatureLeftRatio : 0);

            int blueCardsCount = (int) player.getPlayed().getCards().stream()
                    .map(cardService::getCard)
                    .filter(c -> c.getColor() == CardColor.BLUE)
                    .limit(5)
                    .count();

            out.write((handDevelopedInfrastructure || playedDevelopedInfrastructure) && !planet.isTemperatureMax() ? (10 - (blueCardsCount >= 5 ? 5 : 0)) : 0);
        }

        {
            boolean handGCR = handCardClasses.contains(GasCooledReactors.class);
            boolean playedGCR = playedCardClasses.contains(GasCooledReactors.class);

            out.write(handGCR ? 1 : 0);
            out.write(playedGCR ? 1 : 0);

            out.write((handGCR || playedGCR) ? temperatureLeftRatio : 0);
            out.write((handGCR || playedGCR) && !planet.isTemperatureMax() ? (12 - player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count() * 2) : 0);
        }

        {
            boolean handWI = handCardClasses.contains(WaterImportFromEuropa.class);
            boolean playedWI = playedCardClasses.contains(WaterImportFromEuropa.class);

            boolean handIMI = handCardClasses.contains(IoMiningIndustries.class);
            boolean playedIMI = playedCardClasses.contains(IoMiningIndustries.class);

            boolean handTG = handCardClasses.contains(TerraformingGanymede.class);

            out.write(handWI ? 1 : 0);
            out.write(playedWI ? 1 : 0);

            out.write(handIMI ? 1 : 0);
            out.write(playedIMI ? 1 : 0);

            out.write(playedWI || handWI ? oceansLeftRatio : 0);

            out.write((handWI || playedWI) && !planet.isOceansMax() ? Math.max(0, 12 - player.getTitaniumIncome()) : 0);

            int handJupiterTagCount = handTagCounts[Tag.JUPITER.ordinal()];

            out.write(((handWI || playedWI) ? playedJupiterTagCount : 0) + ((handIMI || playedIMI) ? playedJupiterTagCount : 0));
            out.write(((handWI || playedWI) ? handJupiterTagCount : 0) + ((handIMI || playedIMI) ? handJupiterTagCount : 0));
            out.write(handTG ? playedJupiterTagCount : 0);
        }

        {
            boolean handPP = handCardClasses.contains(ProgressivePolicies.class);
            boolean playedPP = playedCardClasses.contains(ProgressivePolicies.class);

            out.write(handPP ? 1 : 0);
            out.write(playedPP ? 1 : 0);

            out.write(playedPP || handPP ? oxygenLeftRatio : 0);

            out.write((handPP || playedPP) && !planet.isOxygenMax() ? (10 - (playedEvent >= 4 ? 5 : 0)) : 0);
        }

        {
            boolean handIW = handCardClasses.contains(IronWorks.class);
            boolean playedIW = playedCardClasses.contains(IronWorks.class);

            out.write(handIW ? 1 : 0);
            out.write(playedIW ? 1 : 0);
            out.write((playedIW || handIW) ? oxygenLeftRatio : 0);
        }

        {
            boolean handSW = handCardClasses.contains(Steelworks.class);
            boolean playedSW = playedCardClasses.contains(Steelworks.class);

            out.write(handSW ? 1 : 0);
            out.write(playedSW ? 1 : 0);
            out.write((playedSW || handSW) ? oxygenLeftRatio : 0);
        }

        {
            boolean handSP = handCardClasses.contains(SolarPunk.class);
            boolean playedSP = playedCardClasses.contains(SolarPunk.class);

            out.write(handSP ? 1 : 0);
            out.write(playedSP ? 1 : 0);

            out.write(playedSP || handSP ? oxygenLeftRatio : 0);
            out.write((handSP || playedSP) ? Math.max(0, 15 - player.getTitaniumIncome() * 2) : 0);

            //доход говорит о том, что мы в теории можем жать эту карту даже без скидки
            out.write((handSP || playedSP) ? (float) Math.log1p(player.getMcIncome() + player.getTerraformingRating()) : 0);
        }

        {
            boolean handST = handCardClasses.contains(StandardTechnology.class);
            boolean playedST = playedCardClasses.contains(StandardTechnology.class);

            out.write(handST ? 1 : 0);
            out.write(playedST ? 1 : 0);

            float totalProgress = planet.oceansLeft() + planet.temperatureLeft() + planet.oxygenLeft();
            out.write(handST || playedST ? totalProgress : 0);

            out.write((handST || playedST) ? (float) Math.log1p(player.getMcIncome() + player.getTerraformingRating()) : 0);
        }

        {
            boolean handBirds = handCardClasses.contains(Birds.class);
            boolean playedBirds = playedCardClasses.contains(Birds.class);

            out.write(handBirds ? 1 : 0);
            out.write(playedBirds ? 1 : 0);

            int currentOxygenLevel = planet.getCurrentLevel(GlobalParameter.OXYGEN);
            int targetOxygenLevel = getOxygenStepFromColor(ctx.isCanAmplifyOxygenOrTemperature() ? ParameterColor.Y : ParameterColor.W, false);

            float availabilityProgress = (currentOxygenLevel >= targetOxygenLevel) ? 1.0f : ((float) currentOxygenLevel / targetOxygenLevel);
            out.write(handBirds ? availabilityProgress : 0);
        }

        {
            boolean handTardigrades = handCardClasses.contains(Tardigrades.class);
            boolean playedTardigrades = playedCardClasses.contains(Tardigrades.class);

            out.write(handTardigrades ? 1 : 0);
            out.write(playedTardigrades ? 1 : 0);
        }

        {//good example
            boolean handPhysicsComplex = handCardActions.containsKey(CardAction.PHYSICS_COMPLEX);
            boolean playedPhysicsComplex = playedCardActions.containsKey(CardAction.PHYSICS_COMPLEX);

            out.write(handPhysicsComplex ? 1 : 0);
            out.write(playedPhysicsComplex ? 1 : 0);

            out.write(playedPhysicsComplex || handPhysicsComplex ? temperatureLeftRatio : 0);

            out.write(handPhysicsComplex && science >= 4 ? 1 : 0);
            out.write(handPhysicsComplex ? Math.min(1f, science / 4f) : 0);
        }

        {//good example
            boolean plantation = handCardClasses.contains(Plantation.class);
            out.write(plantation ? 1 : 0);
            out.write(plantation && science >= 4 ? 1 : 0);
            out.write(plantation ? Math.min(1f, science / 4f) : 0);
        }

        {
            boolean scienceHand = handCardClasses.contains(MarsUniversity.class);
            boolean scienceTable = playedCardClasses.contains(MarsUniversity.class);

            out.write(scienceTable ? 1f : 0f);
            out.write(scienceHand ? 1f : 0f);

            out.write(scienceHand || scienceTable ? (float) handTagCounts[Tag.SCIENCE.ordinal()] : 0);
            out.write(scienceHand || scienceTable ? (float) handTagCounts[Tag.PLANT.ordinal()] : 0);
        }


        {
            boolean matterGenOnTable = playedCardActions.containsKey(CardAction.MATTER_GENERATOR);
            boolean matterGenInHand = handCardActions.containsKey(CardAction.MATTER_GENERATOR);

            out.write(matterGenOnTable ? 1.0f : 0f);
            out.write(matterGenInHand ? 1.0f : 0f);
            out.write((matterGenOnTable || matterGenInHand) ? hand.size() : 0);
        }

        {
            boolean tableRC = playedCardActions.containsKey(CardAction.REDRAFTED_CONTRACTS);
            boolean handRC = handCardActions.containsKey(CardAction.REDRAFTED_CONTRACTS);
            boolean tableSS = playedCardActions.containsKey(CardAction.SOFTWARE_STREAMLINING);
            boolean handSS = handCardActions.containsKey(CardAction.SOFTWARE_STREAMLINING);

            out.write(tableRC ? 1.0f : 0f);
            out.write(handRC ? 1.0f : 0f);
            out.write(tableSS ? 1.0f : 0f);
            out.write(handSS ? 1.0f : 0f);

            float cyclingUtility = Math.min(1.0f, player.getHand().size() / 10.0f) * Math.max(0.0f, 1.0f - (player.getCardIncome() / 4.0f));
            out.write(tableRC || handRC ? cyclingUtility : 0);
            out.write(tableSS || handSS ? cyclingUtility : 0);
        }

        {
            boolean handAJ = handCardClasses.contains(ArtificialJungle.class);
            boolean tableAJ = playedCardClasses.contains(ArtificialJungle.class);

            boolean handWBS = handCardActions.containsKey(CardAction.WOOD_BURNING_STOVES);
            boolean tableWBS = playedCardActions.containsKey(CardAction.WOOD_BURNING_STOVES);

            out.write(handAJ ? 1 : 0);
            out.write(tableAJ ? 1 : 0);

            out.write((handWBS || tableWBS) ? temperatureLeftRatio : 0);

            out.write((handAJ || tableAJ || handWBS || tableWBS) ? (player.getPlants() > 0 ? 1 : 0) : 0f);
            out.write((handAJ || tableAJ || handWBS || tableWBS) ? ((float) Math.log1p(player.getPlants())) : 0f);
            out.write((handAJ || tableAJ || handWBS || tableWBS) ? (player.getPlantsIncome() > 0 ? 1 : 0) : 0f);
            out.write((handAJ || tableAJ || handWBS || tableWBS) ? ((float) Math.log1p(player.getPlantsIncome())) : 0f);

            out.write((handWBS || tableWBS) && player.getPlants() >= 3 ? 1 : 0);
        }

        {
            out.write(handCardActions.containsKey(CardAction.INTERPLANETARY_RELATIONS) ? 1 : 0);
            out.write(playedCardActions.containsKey(CardAction.INTERPLANETARY_RELATIONS) ? 1 : 0);
            out.write(handCardActions.containsKey(CardAction.INTERPLANETARY_RELATIONS) ? (player.getPlayed().size() - 1) : 0);//we get 1vp per 4 cards, this will be normalized, describe only potential
        }

        {
            boolean handIS = handCardClasses.contains(ImmigrationShuttles.class);
            boolean playedIS = playedCardClasses.contains(ImmigrationShuttles.class);

            out.write(handIS ? 1 : 0);
            out.write(playedIS ? 1 : 0);
            out.write(playedIS ? handTagCounts[Tag.EARTH.ordinal()] : 0);
            out.write(handIS ? handTagCounts[Tag.EARTH.ordinal()] : 0);
            out.write(handIS ? earth : 0);
        }

        {
            boolean advancedEcosystems = handCardClasses.contains(AdvancedEcosystems.class);
            out.write(advancedEcosystems ? 1f : 0f);

            int satisfied = 0;
            if (plant > 0) satisfied++;
            if (microbe > 0) satisfied++;
            if (animal > 0) satisfied++;

            out.write(advancedEcosystems ? satisfied /3f : 0);
        }

        {
            boolean crater = handCardClasses.contains(Crater.class);
            out.write(crater ? 1f : 0f);
            out.write(crater ? Math.min(1f, playedEvent / 3f) : 0f);
        }

        {
            boolean nitrogenRichAsteroid = handCardClasses.contains(NitrogenRichAsteroid.class);
            out.write(nitrogenRichAsteroid ? 1f : 0f);
            out.write(nitrogenRichAsteroid ? Math.min(4f, plant) / 4f: 0f);
        }

        {
            boolean syntheticCatastrophe = handCardClasses.contains(SyntheticCatastrophe.class);
            out.write(syntheticCatastrophe ? 1f : 0f);
            out.write(syntheticCatastrophe ? hand.stream().filter(card -> card.getColor() == CardColor.RED).count() : 0f);
        }

        {
            boolean workCrews = handCardClasses.contains(WorkCrews.class);
            out.write(workCrews ? 1f : 0f);
            out.write(workCrews ? (hand.stream().filter(card -> card.getColor() == CardColor.RED || card.getColor() == CardColor.BLUE).count()) : 0f);
        }

    }

    private int getOxygenStepFromColor(ParameterColor color, boolean isMaxBoundary) {
        return switch (color) {
            case P -> // Purple: 0-2 (Min=0, Max=2)
                    isMaxBoundary ? 2 : 0;
            case R -> // Red: 3-6 (Min=3, Max=6)
                    isMaxBoundary ? 6 : 3;
            case Y -> // Yellow: 7-11 (Min=7, Max=11)
                    isMaxBoundary ? 11 : 7;
            case W -> // White: 12-14 (Min=12, Max=14)
                    isMaxBoundary ? 14 : 12;
            // Если карта не имеет требования по цвету, или требование уже выполнено
        };
    }

    private float getHeatConsumeCapacityOnActions(Map<CardAction, Long> cardActions) {
        float capacity = 0;
        if (cardActions.containsKey(CardAction.IRON_WORKS)) capacity += 4;
        if (cardActions.containsKey(CardAction.STEELWORKS)) capacity += 6;
        if (cardActions.containsKey(CardAction.GREEN_HOUSES)) capacity += 4;
        if (cardActions.containsKey(CardAction.CARETAKER_CONTRACT)) capacity += 8;
        if (cardActions.containsKey(CardAction.DEVELOPMENT_CENTER)) capacity += 2;
        return capacity;
    }
}
