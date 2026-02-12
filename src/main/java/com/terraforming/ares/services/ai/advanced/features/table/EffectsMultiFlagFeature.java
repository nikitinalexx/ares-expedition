package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.green.IoMiningIndustries;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.advanced.EncoderConstants;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EffectsMultiFlagFeature implements FeatureBlock {
    public static final List<String> FEATURE_NAMES = List.of(
            // AI Central / Circuit Board / City Council
            "cards_drawn_from_ai_central_circuit_board_city_council",

            // Aquifer Pumping
            "has_aquifer_pumping",
            "aquifer_pumping_mc_cost",

            // Birds
            "has_birds",
            "birds_resource_count",

            // Developed Infrastructure (MC -> Temperature)
            "can_trade_mc_to_temperature",
            "mc_to_temperature_cost",

            // Earth Catapult / Research Outpost / Hohmann
            "card_cost_reduction_total",

            // Fish
            "has_fish",
            "fish_can_place_ocean",
            "fish_resource_count",

            // Herbivores
            "has_herbivores",
            "herbivores_resource_count",

            // Interplanetary Relations
            "has_interplanetary_relations",
            "played_cards_count_minus_one",

            // Assembly Lines
            "has_assembly_lines",
            "assembly_lines_easy_cards_count",

            // Iron Works
            "iron_works_can_raise_oxygen",

            // Steelworks
            "has_steelworks",
            "steelworks_can_raise_oxygen",

            // Livestock
            "has_livestock",
            "livestock_can_raise_temperature",
            "livestock_resource_count",

            // Matter Generator (sell bonus)
            "matter_generator_sell_bonus",

            // Physics Complex
            "has_physics_complex",
            "physics_complex_can_raise_temperature",
            "physics_complex_resource_count",

            // Progressive Policies (MC -> Oxygen)
            "can_trade_mc_to_oxygen",
            "mc_to_oxygen_cost",

            // Recycled Detritus / Impact Analysis
            "event_card_rebate_total",

            // Small Animals
            "has_small_animals",
            "small_animals_resource_count",

            // Solar Punk
            "has_solar_punk",
            "solar_punk_mc_cost",

            // Volcanic Pools
            "volcanic_pools_can_place_ocean",
            "volcanic_pools_energy_cost",

            // Water Import / Io Mining
            "water_import_can_place_ocean",
            "water_import_titanium_cost",
            "water_import_and_io_mining_synergy",

            // Wood Burning Stoves
            "wood_burning_stoves_can_raise_temperature",

            // Community Afforestation
            "has_community_afforestation",
            "community_afforestation_plant_cost",

            // Fibrous Composite
            "has_fibrous_composite_material",
            "fibrous_composite_resource_count",

            // Gas Cooled Reactors
            "gas_cooled_reactors_can_raise_temperature",
            "gas_cooled_reactors_heat_cost",

            // Research Grant
            "research_grant_dynamic_tags_score",

            // Volcanic Soil
            "volcanic_soil_can_raise_temperature",

            // Zoos
            "has_zoos",
            "zoos_resource_count"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int size() {
        return 50;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Map<CardAction, Long> playedCardActions = ctx.getPlayedCardActions();
        Map<Tag, Long> playedTagToCount = ctx.getPlayedTagToCount();
        Set<Class<?>> playedCardClasses = ctx.getPlayedCardClasses();
        CardService cardService = ctx.getCardService();

        MarsGame game = ctx.getGame();
        Player player = ctx.getPlayer();

        {
            boolean aiCentral = playedCardActions.containsKey(CardAction.AI_CENTRAL);
            boolean circuitBoard = playedCardActions.containsKey(CardAction.CIRCUIT_BOARD);
            boolean cityCouncil = playedCardActions.containsKey(CardAction.CITY_COUNCIL);

            int realGetCards = (aiCentral ? 2 : 0) + (circuitBoard ? 1 : 0);

            if (cityCouncil) {
                realGetCards += (1 + game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count());
            }

            out.write(realGetCards);
        }

        {
            boolean aquiferPumping = playedCardActions.containsKey(CardAction.AQUIFER_PUMPING);
            out.write(aquiferPumping && !game.getPlanet().isOceansMax() ? 1 : 0);
            out.write(aquiferPumping && !game.getPlanet().isOceansMax() ? Math.max(0, 10 - player.getTitaniumIncome() * 2) : (aquiferPumping ? 10 : 0));
        }

        {
            boolean birds = playedCardClasses.contains(Birds.class);
            out.write(birds ? 1 : 0);
            out.write(birds ? player.getCardResourcesCount().get(Birds.class) : 0);
        }

        {
            boolean canTradeMcToTemperature = playedCardActions.containsKey(CardAction.DEVELOPED_INFRASTRUCTURE);
            boolean hasTemperatureLeft = !game.getPlanet().isTemperatureMax();

            out.write(canTradeMcToTemperature && hasTemperatureLeft ? 1 : 0);
            out.write(canTradeMcToTemperature
                    && hasTemperatureLeft ? (player.getPlayed().getCards().stream().map(cardService::getCard).filter(card -> card.getColor() == CardColor.BLUE).limit(5).count() >= 5 ? 5 : 10) : (canTradeMcToTemperature ? 10 : 0));
        }

        {
            boolean earthCatapult = playedCardActions.containsKey(CardAction.EARTH_CATAPULT);
            boolean researchOutpost = playedCardActions.containsKey(CardAction.RESEARCH_OUTPOST);
            boolean hohmann = playedCardClasses.contains(HohmannTransferShipping.class);
            out.write((earthCatapult ? 2 : 0) + (researchOutpost ? 1 : 0) + (hohmann ? 1 : 0));
        }

        {
            boolean fish = playedCardActions.containsKey(CardAction.FISH);
            out.write(fish ? 1 : 0);
            out.write(fish && !game.getPlanet().isOceansMax() ? 1 : 0);
            out.write(fish ? player.getCardResourcesCount().get(Fish.class) : 0);
        }

        {
            boolean herbivores = playedCardActions.containsKey(CardAction.HERBIVORES);
            out.write(herbivores ? 1 : 0);
            out.write(herbivores ? player.getCardResourcesCount().get(Herbivores.class) : 0);
        }

        {
            boolean interplanetaryRelations = playedCardActions.containsKey(CardAction.INTERPLANETARY_RELATIONS);
            out.write(interplanetaryRelations ? 1 : 0);
            out.write(interplanetaryRelations ? (player.getPlayed().size() - 1) : 0);
        }

        {
            boolean assemblyLines = playedCardActions.containsKey(CardAction.ASSEMBLY_LINES);
            out.write(assemblyLines ? 1 : 0);
            out.write(assemblyLines ? playedCardClasses.stream().filter(EncoderConstants.ASSEMBLY_LINES_EASY_CARDS::contains).count() : 0);
        }

        {
            boolean ironWorks = playedCardActions.containsKey(CardAction.IRON_WORKS);
            out.write(ironWorks && !game.getPlanet().isOxygenMax() ? 1 : 0);
        }

        {
            boolean steelWorks = playedCardActions.containsKey(CardAction.STEELWORKS);
            out.write(steelWorks ? 1 : 0);//can get 2mc when spent 6heat
            out.write(steelWorks && !game.getPlanet().isOxygenMax() ? 1 : 0);//loses oxygen raise if it is max
        }

        {
            boolean livestock = playedCardActions.containsKey(CardAction.LIVESTOCK);
            out.write(livestock ? 1 : 0);
            out.write(livestock && !game.getPlanet().isTemperatureMax() ? 1 : 0);
            out.write(livestock ? player.getCardResourcesCount().get(Livestock.class) : 0);
        }

        {
            boolean exocorp = playedCardActions.containsKey(CardAction.EXOCORP_CORPORATION);
            boolean compostingFactory = playedCardActions.containsKey(CardAction.COMPOSTING_FACTORY);
            int cardSellBonus = (exocorp ? 1 : 0) + (compostingFactory ? 1 : 0);

            boolean matterGeneratorSellBonus = playedCardActions.containsKey(CardAction.MATTER_GENERATOR);
            out.write(matterGeneratorSellBonus ? 3 - cardSellBonus : 0);
        }

        {
            boolean physicsComplex = playedCardActions.containsKey(CardAction.PHYSICS_COMPLEX);
            out.write(physicsComplex ? 1 : 0);
            out.write(physicsComplex && !game.getPlanet().isTemperatureMax() ? 1 : 0);
            out.write(physicsComplex ? player.getCardResourcesCount().get(PhysicsComplex.class) : 0);
        }

        {
            boolean canTradeMcToOxygen = playedCardActions.containsKey(CardAction.PROGRESSIVE_POLICIES);
            boolean hasOxygenLeft = !game.getPlanet().isOxygenMax();

            out.write(canTradeMcToOxygen && hasOxygenLeft ? 1 : 0);
            out.write(canTradeMcToOxygen
                    && hasOxygenLeft ? (playedTagToCount.getOrDefault(Tag.EVENT, 0L) >= 4 ? 5 : 10) : (canTradeMcToOxygen ? 10 : 0));
        }

        {
            boolean recycledDetritus = playedCardActions.containsKey(CardAction.RECYCLED_DETRITUS);
            boolean impactAnalysis = playedCardActions.containsKey(CardAction.IMPACT_ANALYSIS);

            out.write((recycledDetritus ? 2 : 0) + (impactAnalysis ? 1 : 0));
        }

        {
            boolean smallAnimals = playedCardActions.containsKey(CardAction.SMALL_ANIMALS);
            out.write(smallAnimals ? 1 : 0);
            out.write(smallAnimals ? player.getCardResourcesCount().get(SmallAnimals.class) : 0);
        }

        {
            boolean solarPunk = playedCardActions.containsKey(CardAction.SOLAR_PUNK);
            out.write(solarPunk ? 1 : 0);
            out.write(solarPunk ? Math.max(0, 15 - player.getTitaniumIncome() * 2) : 0);
        }

        {
            boolean volcanicPools = playedCardActions.containsKey(CardAction.VOLCANIC_POOLS);
            boolean hasOceansLeft = !game.getPlanet().isOceansMax();

            out.write(volcanicPools && hasOceansLeft ? 1 : 0);
            out.write(volcanicPools && hasOceansLeft ? Math.max(0, 12 - playedTagToCount.getOrDefault(Tag.ENERGY, 0L)) : (volcanicPools ? 12 : 0));
        }

        {
            boolean waterImportFromEuropa = playedCardActions.containsKey(CardAction.WATER_IMPORT);
            boolean ioMiningIndustries = playedCardClasses.contains(IoMiningIndustries.class);
            boolean hasOceansLeft = !game.getPlanet().isOceansMax();

            out.write(waterImportFromEuropa && hasOceansLeft ? 1 : 0);
            out.write(waterImportFromEuropa && hasOceansLeft ? Math.max(0, 12 - player.getTitaniumIncome()) : (waterImportFromEuropa ? 12 : 0));
            out.write((waterImportFromEuropa ? 1 : 0) + (ioMiningIndustries ? 1 : 0));
        }

        {
            boolean woodBurningStoves = playedCardActions.containsKey(CardAction.WOOD_BURNING_STOVES);
            out.write(woodBurningStoves && !game.getPlanet().isTemperatureMax() ? 1 : 0);
        }

        {
            boolean afforestation = playedCardActions.containsKey(CardAction.COMMUNITY_AFFORESTATION);

            out.write(afforestation ? 1 : 0);
            out.write(afforestation ? Math.max(0, 14 - game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count() * 4) : 0);
        }

        {
            boolean fibrousComposite = playedCardActions.containsKey(CardAction.FIBROUS_COMPOSITE_MATERIAL);
            out.write(fibrousComposite ? 1 : 0);
            out.write(fibrousComposite ? player.getCardResourcesCount().get(FibrousCompositeMaterial.class) : 0);
        }

        {
            boolean gasCooledReactors = playedCardActions.containsKey(CardAction.GAS_COOLED_REACTORS);
            boolean hasTemperatureLeft = !game.getPlanet().isTemperatureMax();

            out.write(gasCooledReactors && hasTemperatureLeft ? 1 : 0);
            out.write(gasCooledReactors && hasTemperatureLeft ? (12 - player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count() * 2) : (gasCooledReactors ? 12 : 0));
        }

        {
            boolean researchGrant = playedCardActions.containsKey(CardAction.RESEARCH_GRANT);
            if (researchGrant) {
                final List<Tag> cardTags = player.getCardToTag().get(ResearchGrant.class);
                int dynamicFlagCount = 0;
                for (Tag cardTag : cardTags) {
                    if (cardTag == Tag.DYNAMIC) {
                        dynamicFlagCount++;
                    }
                }
                out.write(dynamicFlagCount / 3f);
            } else {
                out.write(0);
            }
        }

        {
            boolean volcanicSoil = playedCardActions.containsKey(CardAction.VOLCANIC_SOIL);
            out.write(volcanicSoil && !game.getPlanet().isTemperatureMax() ? 1 : 0);
        }

        {
            boolean zoos = playedCardActions.containsKey(CardAction.ZOOS);
            out.write(zoos ? 1 : 0);
            out.write(zoos ? player.getCardResourcesCount().get(Zoos.class) : 0);
        }
    }
}
