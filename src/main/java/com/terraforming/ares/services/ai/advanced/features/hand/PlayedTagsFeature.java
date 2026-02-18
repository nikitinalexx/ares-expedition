package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;

public class PlayedTagsFeature implements FeatureBlock {
    @Override
    public int size() {
        return 10;
    }

    public static final List<String> FEATURE_NAMES = List.of(
            "played_tag_space",
            "played_tag_earth",
            "played_tag_event",
            "played_tag_science",
            "played_tag_plant",
            "played_tag_energy",
            "played_tag_building",
            "played_tag_animal",
            "played_tag_jupiter",
            "played_tag_microbe"
    );


    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        int[] tagCounts = ctx.getPlayedTagToCount();

        int earth     = tagCounts[Tag.EARTH.ordinal()];
        int animal    = tagCounts[Tag.ANIMAL.ordinal()];
        int plant     = tagCounts[Tag.PLANT.ordinal()];
        int science   = tagCounts[Tag.SCIENCE.ordinal()];
        int building  = tagCounts[Tag.BUILDING.ordinal()];
        int energy    = tagCounts[Tag.ENERGY.ordinal()];
        int space     = tagCounts[Tag.SPACE.ordinal()];
        int event     = tagCounts[Tag.EVENT.ordinal()];
        int microbe   = tagCounts[Tag.MICROBE.ordinal()];
        int jupiter   = tagCounts[Tag.JUPITER.ordinal()];

        out.write(space);
        out.write(earth);
        out.write(event);
        out.write(science);
        out.write(plant);
        out.write(energy);
        out.write(building);
        out.write(animal);
        out.write(jupiter);
        out.write(microbe);
    }

}
