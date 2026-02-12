package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TableMicrobesFeature implements FeatureBlock {
    public static final List<String> MICROBE_FEATURE_NAMES = List.of(
            // ANAEROBIC_MICROORGANISMS
            "anaerobic_microorganisms_present",
            "anaerobic_microorganisms_microbes",

            // CONSERVED_BIOME
            "conserved_biome_present",
            "conserved_biome_forest_vp",

            // DECOMPOSERS
            "decomposers_present",
            "decomposers_microbes",

            // DECOMPOSING_FUNGUS
            "decomposing_fungus_present",
            "decomposing_fungus_microbes",

            // ECOLOGICAL_ZONE
            "ecological_zone_present",
            "ecological_zone_resources",

            // EXTREME_COLD_FUNGUS
            "extreme_cold_fungus_present",

            // FILTER_FEEDERS
            "filter_feeders_present",
            "filter_feeders_resources",

            // GHG_PRODUCTION
            "ghg_production_present",
            "ghg_production_temperature_not_max",
            "ghg_production_microbes",

            // NITRITE_REDUCTING
            "nitrite_reducting_present",
            "nitrite_reducting_oceans_not_max",
            "nitrite_reducting_microbes",

            // REGOLITH_EATERS
            "regolith_eaters_present",
            "regolith_eaters_oxygen_not_max",
            "regolith_eaters_microbes",

            // SELF_REPLICATING_BACTERIA
            "self_replicating_bacteria_present",
            "self_replicating_bacteria_microbes",

            // SYMBIOTIC_FUNGUS
            "symbiotic_fungus_present",

            // TARDIGRADES
            "tardigrades_present",
            "tardigrades_microbes",

            // VIRAL_ENHANCERS
            "viral_enhancers_present",

            // BACTERIAL_AGGREGATES
            "bacterial_aggregates_present",
            "bacterial_aggregates_microbes",

            // AGGREGATES / TOTALS
            "activates_on_microbe_tag_count",
            "activates_on_animal_or_plant_tag_count",
            "total_microbe_generation_strength",
            "gives_microbe_to_another_capacity"
    );

    @Override
    public List<String> getFeatureNames() {
        return MICROBE_FEATURE_NAMES;
    }

    @Override
    public int size() {
        return 34;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Map<CardAction, Long> playedCardActions = ctx.getPlayedCardActions();
        Set<Class<?>> playedCardClasses = ctx.getPlayedCardClasses();

        Player player = ctx.getPlayer();
        MarsGame game = ctx.getGame();

        //MICROBES BLOCK
        int totalMicrobeGenerationStrength = 0;
        int activatesOnMicrobeTag = 0;
        int activatesOnAnimalPlantTag = 0;
        int givesMicrobeToAnotherCapacity = 0;

        {
            boolean anaerobicMicroorganisms = playedCardActions.containsKey(CardAction.ANAEROBIC_MICROORGANISMS);
            if (anaerobicMicroorganisms) {
                out.write(1);
                out.write(player.getCardResourcesCount().get(AnaerobicMicroorganisms.class));
                activatesOnMicrobeTag++;
                activatesOnAnimalPlantTag++;
                totalMicrobeGenerationStrength++;
            } else {
                out.write(0);
                out.write(0);
            }
        }

        {
            boolean conservedBiome = playedCardActions.containsKey(CardAction.CONSERVED_BIOME);
            if (conservedBiome) {
                out.write(1);//also means animalGenerationPower, only card that has it
                out.write(player.getForests() / 2f);//gives 0.5vp per forest
                givesMicrobeToAnotherCapacity++;
            } else {
                out.write(0);
                out.write(0);
            }
        }

        {
            boolean decomposers = playedCardActions.containsKey(CardAction.DECOMPOSERS);
            if (decomposers) {
                out.write(1);//also means can trade micrbe to card, only card that has it
                out.write(player.getCardResourcesCount().get(Decomposers.class));
                activatesOnMicrobeTag++;
                activatesOnAnimalPlantTag++;
                totalMicrobeGenerationStrength++;
            } else {
                out.write(0);
                out.write(0);
            }
        }
        {
            boolean decomposingFungus = playedCardActions.containsKey(CardAction.DECOMPOSING_FUNGUS);
            if (decomposingFungus) {
                out.write(1);//also means can turn any microbe/animal to 3 plants, only card that has it
                out.write(player.getCardResourcesCount().get(DecomposingFungus.class));
            } else {
                out.write(0);
                out.write(0);
            }
        }
        {
            boolean ecologicalZone = playedCardActions.containsKey(CardAction.ECOLOGICAL_ZONE);
            if (ecologicalZone) {
                out.write(1);
                out.write(player.getCardResourcesCount().get(EcologicalZone.class));//how many we collected so far
                activatesOnAnimalPlantTag++;
            } else {
                out.write(0);
                out.write(0);
            }
        }

        {
            boolean extremeColdFungus = playedCardActions.containsKey(CardAction.EXTREME_COLD_FUNGUS);
            if (extremeColdFungus) {
                out.write(1);//also means action OR get 1 plant
                givesMicrobeToAnotherCapacity++;
            } else {
                out.write(0);
            }
        }

        {
            boolean filterFeeders = playedCardActions.containsKey(CardAction.FILTER_FEEDERS);
            if (filterFeeders) {
                out.write(1);//unique effect it has
                out.write(player.getCardResourcesCount().get(FilterFeeders.class));
            } else {
                out.write(0);
                out.write(0);
            }
        }

        {
            boolean ghgProduction = playedCardActions.containsKey(CardAction.GHG_PRODUCTION);
            if (ghgProduction) {
                out.write(1);
                out.write(game.getPlanet().isTemperatureMax() ? 0 : 1);
                out.write(player.getCardResourcesCount().get(GhgProductionBacteria.class));
                totalMicrobeGenerationStrength++;
            } else {
                out.write(0);
                out.write(0);
                out.write(0);
            }
        }

        {
            boolean nitriteReducting = playedCardActions.containsKey(CardAction.NITRITE_REDUCTING);
            if (nitriteReducting) {
                out.write(1);
                out.write(game.getPlanet().isOceansMax() ? 0 : 1);
                out.write(player.getCardResourcesCount().get(NitriteReductingBacteria.class));
                totalMicrobeGenerationStrength++;
            } else {
                out.write(0);
                out.write(0);
                out.write(0);
            }
        }

        {
            boolean regolithEaters = playedCardActions.containsKey(CardAction.REGOLITH_EATERS);
            if (regolithEaters) {
                out.write(1);
                out.write(game.getPlanet().isOxygenMax() ? 0 : 1);
                out.write(player.getCardResourcesCount().get(RegolithEaters.class));
                totalMicrobeGenerationStrength++;
            } else {
                out.write(0);
                out.write(0);
                out.write(0);
            }
        }

        {
            boolean selfReplicatingBacteria = playedCardActions.containsKey(CardAction.SELF_REPLICATING_BACTERIA);
            if (selfReplicatingBacteria) {
                out.write(1);//also means a -25mc discount option
                out.write(player.getCardResourcesCount().get(SelfReplicatingBacteria.class));
                totalMicrobeGenerationStrength++;
            } else {
                out.write(0);
                out.write(0);
            }
        }

        {
            boolean symbioticFungus = playedCardActions.containsKey(CardAction.SYMBIOTIC_FUNGUD);
            if (symbioticFungus) {
                out.write(1);
            } else {
                out.write(0);
            }
        }

        {
            boolean tardigrades = playedCardClasses.contains(Tardigrades.class);
            if (tardigrades) {
                out.write(1);
                out.write(player.getCardResourcesCount().get(Tardigrades.class));
                totalMicrobeGenerationStrength++;
            } else {
                out.write(0);
                out.write(0);
            }
        }

        {
            boolean viralEnhancers = playedCardActions.containsKey(CardAction.VIRAL_ENHANCERS);
            if (viralEnhancers) {
                out.write(1);
                activatesOnMicrobeTag++;
                activatesOnAnimalPlantTag++;
                totalMicrobeGenerationStrength++;
            } else {
                out.write(0);
            }
        }

        {
            boolean bacterialAggregates = playedCardActions.containsKey(CardAction.BACTERIAL_AGGREGATES);
            if (bacterialAggregates) {
                out.write(1);//also means activates on Earth tag
                Integer microbesAccumulated = player.getCardResourcesCount().get(BacterialAggregates.class);
                out.write(microbesAccumulated);
                if (microbesAccumulated < 5) {
                    totalMicrobeGenerationStrength++;
                }
            } else {
                out.write(0);
                out.write(0);
            }
        }

        out.write(activatesOnMicrobeTag);
        out.write(activatesOnAnimalPlantTag);
        out.write(totalMicrobeGenerationStrength);
        out.write(givesMicrobeToAnotherCapacity);
    }

}
