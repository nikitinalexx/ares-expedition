package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TableResourcesFeature implements FeatureBlock {
    public static final List<String> MICROBE_FEATURE_NAMES = List.of(
            "research_grant_dynamic_left",

            "birds_animal_count",
            "filterfeeders_animal_count",
            "fish_animal_count",
            "livestock_animal_count",
            "zoos_animal_count",
            "ecologicalzone_animal_count",
            "herbivores_animal_count",
            "smallanimals_animal_count",

            "anaerobic_microbe_count",
            "decomposers_microbe_count",
            "decomposing_fungus_microbe_count",
            "ghg_microbe_count",
            "nitrite_microbe_count",
            "regolith_microbe_count",
            "self_replicating_microbe_count",
            "tardigrades_microbe_count",
            "bacterial_aggregates_microbe_count"
    );

    @Override
    public List<String> getFeatureNames() {
        return MICROBE_FEATURE_NAMES;
    }

    @Override
    public int size() {
        return 18;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Map<CardAction, Long> playedCardActions = ctx.getPlayedCardActions();

        Player player = ctx.getPlayer();

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

        //ANIMALS BLOCK
        {
            out.write(player.getCardResourcesCount().getOrDefault(Birds.class, 0) + player.getCardResourcesCount().getOrDefault(BuffedBirds.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(FilterFeeders.class, 0) + player.getCardResourcesCount().getOrDefault(BuffedFilterFeeders.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(Fish.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(Livestock.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(Zoos.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(EcologicalZone.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(Herbivores.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(SmallAnimals.class, 0));
        }

        //MICROBES BLOCK
        {
            out.write(player.getCardResourcesCount().getOrDefault(AnaerobicMicroorganisms.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(Decomposers.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(DecomposingFungus.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(GhgProductionBacteria.class, 0) + player.getCardResourcesCount().getOrDefault(BuffedGhgProductionBacteria.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(NitriteReductingBacteria.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(RegolithEaters.class, 0) + player.getCardResourcesCount().getOrDefault(BuffedRegolithEaters.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(SelfReplicatingBacteria.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(Tardigrades.class, 0));
            out.write(player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0));
        }
    }

}
