package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;

public class MilestonesFeature implements FeatureBlock {
    private static final List<String> FEATURE_NAMES = List.of(
            "table_milest_1_my_progress",
            "table_milest_1_opp_progress",
            "table_milest_1_ach_progress",
            "table_milest_1_diff_progress",

            "table_milest_2_my_progress",
            "table_milest_2_opp_progress",
            "table_milest_2_ach_progress",
            "table_milest_2_diff_progress",

            "table_milest_3_my_progress",
            "table_milest_3_opp_progress",
            "table_milest_3_ach_progress",
            "table_milest_3_diff_progress"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int size() {
        return 12;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        MarsGame game = ctx.getGame();
        Player player = ctx.getPlayer();
        Player opponent = ctx.getOpponent();

        CardService cardService = ctx.getCardService();

        List<Milestone> milestones = game.getMilestones();
        for (Milestone m : milestones) {
            // 1. Мой прогресс (0.0 - 1.0)
            float myProgress = (float) m.getValue(player, cardService) / m.getMaxValue();
            out.write(myProgress);

            // 2. Прогресс оппонента
            float opponentProgress = (float) m.getValue(opponent, cardService) / m.getMaxValue();
            out.write(opponentProgress);

            boolean achieved = m.isAchieved();
            out.write(achieved ? 1.0f : 0.0f);
            out.write(achieved ? 0 : myProgress - opponentProgress);
        }
    }

}
