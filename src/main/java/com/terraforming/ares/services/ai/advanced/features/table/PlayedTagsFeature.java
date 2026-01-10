package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;

public class PlayedTagsFeature implements FeatureBlock {
    private static final List<String> FEATURE_NAMES = List.of(
            "table_played_space",
            "table_played_earth",
            "table_played_event",
            "table_played_science",
            "table_played_plant",
            "table_played_energy",
            "table_played_building",
            "table_played_animal",
            "table_played_jupiter",
            "table_played_microbe"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int size() {
        return 10;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Map<Tag, Long> playedTagToCount = ctx.getPlayedTagToCount();

        out.write(playedTagToCount.getOrDefault(Tag.SPACE, 0L).intValue());
        out.write(playedTagToCount.getOrDefault(Tag.EARTH, 0L).intValue());
        out.write(playedTagToCount.getOrDefault(Tag.EVENT, 0L).intValue());
        out.write(playedTagToCount.getOrDefault(Tag.SCIENCE, 0L).intValue());
        out.write(playedTagToCount.getOrDefault(Tag.PLANT, 0L).intValue());
        out.write(playedTagToCount.getOrDefault(Tag.ENERGY, 0L).intValue());
        out.write(playedTagToCount.getOrDefault(Tag.BUILDING, 0L).intValue());
        out.write(playedTagToCount.getOrDefault(Tag.ANIMAL, 0L).intValue());
        out.write(playedTagToCount.getOrDefault(Tag.JUPITER, 0L).intValue());
        out.write(playedTagToCount.getOrDefault(Tag.MICROBE, 0L).intValue());
    }
}
