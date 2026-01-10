package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;

public class PhaseUpgradeFeature implements FeatureBlock {
    private static final List<String> FEATURE_NAMES = List.of(
            "table_phase_1_app_1",
            "table_phase_1_app_2",

            "table_phase_2_app_1",
            "table_phase_2_app_2",

            "table_phase_3_app_1",
            "table_phase_3_app_2",

            "table_phase_4_app_1",
            "table_phase_4_app_2",

            "table_phase_5_app_1",
            "table_phase_5_app_2"
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
        Player player = ctx.getPlayer();

        for (int i = 0; i < 5; i++) {
            out.write(player.getPhaseCards().get(i) == 1 ? 1 : 0);
            out.write(player.getPhaseCards().get(i) == 2 ? 1 : 0);
        }
    }

}
