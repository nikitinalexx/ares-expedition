package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;

public class HandTagsFeature implements FeatureBlock {
    @Override
    public int size() {
        return 13;
    }

    public static final List<String> FEATURE_NAMES = List.of(
            "hand_space_count",
            "hand_earth_count",
            "hand_event_count",
            "hand_science_count",
            "hand_plant_count",
            "hand_energy_count",
            "hand_building_count",
            "hand_animal_count",
            "hand_jupiter_count",
            "hand_microbe_count",
            "hand_dynamic_count",

            "hand_unique_tags",
            "max_tag_concentration"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        int[] handTagCounts = ctx.getHandTagCounts();

        // --- 1.5 Теги (количество каждого тега в руке) ---
        for (int i = 0; i < handTagCounts.length; i++) {
            out.write(handTagCounts[i]);
        }

        // --- 1.6 Тег diversity ---
        int uniqueTags = 0;
        for (int count : handTagCounts) {
            if (count > 0) uniqueTags++;
        }
        out.write(uniqueTags);          //Разнообразие тегов

        // --- 1.7 Самый частый тег ---
        int maxTagCount = 0;
        for (int count : handTagCounts) {
            maxTagCount = Math.max(maxTagCount, count);
        }
        out.write(maxTagCount);         //Max tag concentration
    }
}
