package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;

public class PolicyPlayedTagsFeature implements PlayerPolicyFeatureBlock {

    @Override
    public void encode(TableContext ctx, PolicyRecord out, int playerIndex) {
        int[] playedTagToCount = ctx.getPlayedTagToCount();

        out.spaceTags[playerIndex]    = (byte) playedTagToCount[Tag.SPACE.ordinal()];
        out.earthTags[playerIndex]    = (byte) playedTagToCount[Tag.EARTH.ordinal()];
        out.eventTags[playerIndex]    = (byte) playedTagToCount[Tag.EVENT.ordinal()];
        out.scienceTags[playerIndex]  = (byte) playedTagToCount[Tag.SCIENCE.ordinal()];
        out.plantTags[playerIndex]    = (byte) playedTagToCount[Tag.PLANT.ordinal()];
        out.energyTags[playerIndex]   = (byte) playedTagToCount[Tag.ENERGY.ordinal()];
        out.buildingTags[playerIndex] = (byte) playedTagToCount[Tag.BUILDING.ordinal()];
        out.animalTags[playerIndex]   = (byte) playedTagToCount[Tag.ANIMAL.ordinal()];
        out.jupiterTags[playerIndex]  = (byte) playedTagToCount[Tag.JUPITER.ordinal()];
        out.microbeTags[playerIndex]  = (byte) playedTagToCount[Tag.MICROBE.ordinal()];
    }

}
