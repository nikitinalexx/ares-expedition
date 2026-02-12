package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;

public class TerraformingFeature implements FeatureBlock {
    public static final List<String> FEATURE_NAMES = List.of(
            "oxygen_phase_start",
            "oxygen_phase_start_neg",

            "temp_phase_start",
            "temp_phase_start_neg",

            "oceans_phase_start",
            "oceans_phase_start_neg"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int size() {
        return 6;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        MarsGame game = ctx.getGame();
        Planet planet = game.getPlanet();

        float oxygenLeftRatio = (float) planet.oxygenLeft() / planet.oxygenMax();
        float temperatureLeftRatio = (float) planet.temperatureLeft() / planet.temperatureMax();
        float oceansLeftRatio = (float) planet.oceansLeft() / planet.oceansMaxCount();

        out.write(oxygenLeftRatio);
        out.write(1f - oxygenLeftRatio);

        out.write(temperatureLeftRatio);
        out.write(1f - temperatureLeftRatio);

        out.write(oceansLeftRatio);
        out.write(1f - oceansLeftRatio);
    }

}
