package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;

public class TerraformingFeature implements FeatureBlock {
    public static final List<String> FEATURE_NAMES = List.of(
            "oxygen_left",
            "temperature_left",
            "oceans_left"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        MarsGame game = ctx.getGame();
        Planet planet = game.getPlanet();

        float oxygenLeftRatio = (float) planet.oxygenLeft() / planet.oxygenMax();
        float temperatureLeftRatio = (float) planet.temperatureLeft() / planet.temperatureMax();
        float oceansLeftRatio = (float) planet.oceansLeft() / planet.oceansMaxCount();

        out.write(oxygenLeftRatio);
        out.write(temperatureLeftRatio);
        out.write(oceansLeftRatio);
    }

}
