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
        int[] playedTagToCount = ctx.getPlayedTagToCount();

        out.write(playedTagToCount[Tag.SPACE.ordinal()]);
        out.write(playedTagToCount[Tag.EARTH.ordinal()]);
        out.write(playedTagToCount[Tag.EVENT.ordinal()]);
        out.write(playedTagToCount[Tag.SCIENCE.ordinal()]);
        out.write(playedTagToCount[Tag.PLANT.ordinal()]);
        out.write(playedTagToCount[Tag.ENERGY.ordinal()]);
        out.write(playedTagToCount[Tag.BUILDING.ordinal()]);
        out.write(playedTagToCount[Tag.ANIMAL.ordinal()]);
        out.write(playedTagToCount[Tag.JUPITER.ordinal()]);
        out.write(playedTagToCount[Tag.MICROBE.ordinal()]);
    }
}
