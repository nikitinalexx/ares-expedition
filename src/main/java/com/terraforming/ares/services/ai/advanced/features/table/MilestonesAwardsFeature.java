package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.awards.AbstractAward;
import com.terraforming.ares.model.awards.AwardType;
import com.terraforming.ares.model.awards.BaseAward;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.model.milestones.MilestoneType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class MilestonesAwardsFeature implements FeatureBlock {
    private static final List<String> FEATURE_NAMES = List.of(
            // Milestones (each: isInGame, myProgress, opponentProgress, isAustellarPick)
            "milestone_DIVERSIFIER_isInGame",
            "milestone_DIVERSIFIER_myProgress",
            "milestone_DIVERSIFIER_opponentProgress",
            "milestone_DIVERSIFIER_isAustellarPick",

            "milestone_ENERGIZER_isInGame",
            "milestone_ENERGIZER_myProgress",
            "milestone_ENERGIZER_opponentProgress",
            "milestone_ENERGIZER_isAustellarPick",

            "milestone_FARMER_isInGame",
            "milestone_FARMER_myProgress",
            "milestone_FARMER_opponentProgress",
            "milestone_FARMER_isAustellarPick",

            "milestone_LEGEND_isInGame",
            "milestone_LEGEND_myProgress",
            "milestone_LEGEND_opponentProgress",
            "milestone_LEGEND_isAustellarPick",

            "milestone_MAGNATE_isInGame",
            "milestone_MAGNATE_myProgress",
            "milestone_MAGNATE_opponentProgress",
            "milestone_MAGNATE_isAustellarPick",

            "milestone_PLANNER_isInGame",
            "milestone_PLANNER_myProgress",
            "milestone_PLANNER_opponentProgress",
            "milestone_PLANNER_isAustellarPick",

            "milestone_SPACE_BARON_isInGame",
            "milestone_SPACE_BARON_myProgress",
            "milestone_SPACE_BARON_opponentProgress",
            "milestone_SPACE_BARON_isAustellarPick",

            "milestone_TERRAFORMER_isInGame",
            "milestone_TERRAFORMER_myProgress",
            "milestone_TERRAFORMER_opponentProgress",
            "milestone_TERRAFORMER_isAustellarPick",

            "milestone_TYCOON_isInGame",
            "milestone_TYCOON_myProgress",
            "milestone_TYCOON_opponentProgress",
            "milestone_TYCOON_isAustellarPick",

            "milestone_GARDENER_isInGame",
            "milestone_GARDENER_myProgress",
            "milestone_GARDENER_opponentProgress",
            "milestone_GARDENER_isAustellarPick",

            // Awards (each: isInGame, myProgress_log, opponentProgress_log)
            "award_CELEBRITY_isInGame",
            "award_CELEBRITY_myProgress_log",
            "award_CELEBRITY_opponentProgress_log",

            "award_COLLECTOR_isInGame",
            "award_COLLECTOR_myProgress_log",
            "award_COLLECTOR_opponentProgress_log",

            "award_GENERATOR_isInGame",
            "award_GENERATOR_myProgress_log",
            "award_GENERATOR_opponentProgress_log",

            "award_INDUSTRIALIST_isInGame",
            "award_INDUSTRIALIST_myProgress_log",
            "award_INDUSTRIALIST_opponentProgress_log",

            "award_PROJECT_MANAGER_isInGame",
            "award_PROJECT_MANAGER_myProgress_log",
            "award_PROJECT_MANAGER_opponentProgress_log",

            "award_RESEARCHER_isInGame",
            "award_RESEARCHER_myProgress_log",
            "award_RESEARCHER_opponentProgress_log"

    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int size() {
        return 58;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        MarsGame game = ctx.getGame();
        Player player = ctx.getPlayer();
        Player opponent = ctx.getOpponent();

        CardService cardService = ctx.getCardService();

        Map<MilestoneType, Milestone> typeToMilestone = game.getMilestones().stream().collect(Collectors.toMap(Milestone::getType, Function.identity()));
        for (MilestoneType milestoneType : AiConstants.MILESTONE_TYPES) {
            if (!typeToMilestone.containsKey(milestoneType) || typeToMilestone.get(milestoneType).isAchieved()) {
                out.write(0f);
                out.write(0f);
                out.write(0f);
                out.write(0f);
                continue;
            }
            Milestone m = typeToMilestone.get(milestoneType);

            float myProgress = (float) m.getValue(player, cardService) / m.getMaxValue();
            float opponentProgress = (float) m.getValue(opponent, cardService) / m.getMaxValue();

            out.write(1f); // isInGame
            out.write(Math.min(1f, myProgress));
            out.write(Math.min(1f, opponentProgress));
            out.write((player.getAustellarMilestone() >= 0 && game.getMilestones().get(player.getAustellarMilestone()) == m) ? 1 : 0);
        }

        Map<AwardType, BaseAward> typeToAward = game.getAwards().stream().collect(Collectors.toMap(BaseAward::getType, Function.identity()));
        for (AwardType awardType : AiConstants.AWARD_TYPES) {
            if (!typeToAward.containsKey(awardType)) {
                out.write(0f);
                out.write(0f);
                out.write(0f);
                continue;
            }
            BaseAward award = typeToAward.get(awardType);
            ToIntFunction<Player> awardProgressExtractor = ((AbstractAward) award).comparableParamExtractor(cardService);

            out.write(1f);
            out.write((float) Math.log1p(awardProgressExtractor.applyAsInt(player)));
            out.write((float) Math.log1p(awardProgressExtractor.applyAsInt(opponent)));
        }
    }

}
