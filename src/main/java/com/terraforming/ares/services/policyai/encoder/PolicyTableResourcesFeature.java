package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;
import java.util.Map;

public class PolicyTableResourcesFeature implements PlayerPolicyFeatureBlock {
    private static final int BIRDS = 0;
    private static final int FILTER_FEEDERS = 1;
    private static final int FISH = 2;
    private static final int LIVESTOCK = 3;
    private static final int ZOOS = 4;
    private static final int ECOLOGICAL_ZONE = 5;
    private static final int HERBIVORES = 6;
    private static final int SMALL_ANIMALS = 7;

    private static final int ANAEROBIC = 0;
    private static final int DECOMPOSERS = 1;
    private static final int DECOMPOSING_FUNGUS = 2;
    private static final int GHG = 3;
    private static final int NITRITE = 4;
    private static final int REGOLITH = 5;
    private static final int SELF_REPLICATING = 6;
    private static final int TARDIGRADES = 7;
    private static final int BACTERIAL_AGGREGATES = 8;

    @Override
    public void encode(TableContext ctx, PolicyRecord dto, int playerIndex) {
        Map<CardAction, Long> played =
                ctx.getPlayedCardActions();

        Player player = ctx.getPlayer();

        var resources = player.getCardResourcesCount();

        // =====================
        // RESEARCH GRANT
        // =====================
        if (played.containsKey(CardAction.RESEARCH_GRANT)) {

            int dynamic = 0;

            List<Tag> tags =
                    player.getCardToTag().get(ResearchGrant.class);

            if (tags != null) {
                for (Tag t : tags) {
                    if (t == Tag.DYNAMIC)
                        dynamic++;
                }
            }

            dto.researchGrantDynamicTags[playerIndex] =
                    (byte) dynamic;
        } else {
            dto.researchGrantDynamicTags[playerIndex] = 0;
        }


        // =====================
        // ANIMALS
        // =====================
        byte[] animals =
                dto.animalResources[playerIndex];

        animals[BIRDS] =
                (byte)(
                        resources.getOrDefault(Birds.class,0)
                                + resources.getOrDefault(BuffedBirds.class,0));

        animals[FILTER_FEEDERS] =
                (byte)(
                        resources.getOrDefault(FilterFeeders.class,0)
                                + resources.getOrDefault(BuffedFilterFeeders.class,0));

        animals[FISH] =
                resources.getOrDefault(Fish.class,0).byteValue();

        animals[LIVESTOCK] =
                resources.getOrDefault(Livestock.class,0).byteValue();

        animals[ZOOS] =
                resources.getOrDefault(Zoos.class,0).byteValue();

        animals[ECOLOGICAL_ZONE] =
                resources.getOrDefault(EcologicalZone.class,0).byteValue();

        animals[HERBIVORES] =
                resources.getOrDefault(Herbivores.class,0).byteValue();

        animals[SMALL_ANIMALS] =
                resources.getOrDefault(SmallAnimals.class,0).byteValue();


        // =====================
        // MICROBES
        // =====================
        byte[] microbes =
                dto.microbeResources[playerIndex];

        microbes[ANAEROBIC] =
                resources.getOrDefault(AnaerobicMicroorganisms.class,0).byteValue();

        microbes[DECOMPOSERS] =
                resources.getOrDefault(Decomposers.class,0).byteValue();

        microbes[DECOMPOSING_FUNGUS] =
                resources.getOrDefault(DecomposingFungus.class,0).byteValue();

        microbes[GHG] =
                (byte)(
                        resources.getOrDefault(GhgProductionBacteria.class,0)
                                + resources.getOrDefault(BuffedGhgProductionBacteria.class,0));

        microbes[NITRITE] =
                resources.getOrDefault(NitriteReductingBacteria.class,0).byteValue();

        microbes[REGOLITH] =
                (byte)(
                        resources.getOrDefault(RegolithEaters.class,0)
                                + resources.getOrDefault(BuffedRegolithEaters.class,0));

        microbes[SELF_REPLICATING] =
                resources.getOrDefault(SelfReplicatingBacteria.class,0).byteValue();

        microbes[TARDIGRADES] =
                resources.getOrDefault(Tardigrades.class,0).byteValue();

        microbes[BACTERIAL_AGGREGATES] =
                resources.getOrDefault(BacterialAggregates.class,0).byteValue();
    }

}
