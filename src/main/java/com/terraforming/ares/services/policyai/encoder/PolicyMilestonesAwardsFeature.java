package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.awards.AbstractAward;
import com.terraforming.ares.model.awards.AwardType;
import com.terraforming.ares.model.awards.BaseAward;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.model.milestones.MilestoneType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class PolicyMilestonesAwardsFeature implements PolicyFeatureBlock {

    @Override
    public void encode(TableContext ctx, PolicyRecord out) {
        MarsGame game = ctx.getGame();
        Player player = ctx.getPlayer();
        Player opponent = ctx.getOpponent();
        CardService cardService = ctx.getCardService();

        Map<MilestoneType, Milestone> typeToMilestone =
                game.getMilestones()
                        .stream()
                        .collect(Collectors.toMap(
                                Milestone::getType,
                                Function.identity()
                        ));

        for (int i = 0; i < AiConstants.MILESTONE_TYPES.size(); i++) {

            MilestoneType type = AiConstants.MILESTONE_TYPES.get(i);
            Milestone m = typeToMilestone.get(type);

            if (m == null || m.isAchieved()) {
                continue;
            }

            out.milestoneInGame[i] = 1;

            float myProgress =
                    (float) m.getValue(player, cardService) / m.getMaxValue();

            float opponentProgress =
                    (float) m.getValue(opponent, cardService) / m.getMaxValue();

            out.milestoneMyProgress[i] =
                    Math.min(1f, myProgress);

            out.milestoneOpponentProgress[i] =
                    Math.min(1f, opponentProgress);

            byte austellarOwner = 0;

            int myIdx = player.getAustellarMilestone();
            if (myIdx >= 0 &&
                    game.getMilestones().get(myIdx) == m) {
                austellarOwner = 1;
            }

            int oppIdx = opponent.getAustellarMilestone();
            if (oppIdx >= 0 &&
                    game.getMilestones().get(oppIdx) == m) {
                austellarOwner = -1;
            }

            out.austellarMilestoneOwner[i] = austellarOwner;
        }

        Map<AwardType, BaseAward> typeToAward =
                game.getAwards()
                        .stream()
                        .collect(Collectors.toMap(
                                BaseAward::getType,
                                Function.identity()
                        ));

        for (int i = 0; i < AiConstants.AWARD_TYPES.size(); i++) {

            AwardType type = AiConstants.AWARD_TYPES.get(i);
            BaseAward award = typeToAward.get(type);

            if (award == null) {
                continue;
            }

            out.awardInGame[i] = 1;

            ToIntFunction<Player> extractor =
                    ((AbstractAward) award)
                            .comparableParamExtractor(cardService);

            out.awardMyProgress[i] =
                    (float) Math.log1p(
                            extractor.applyAsInt(player));

            out.awardOpponentProgress[i] =
                    (float) Math.log1p(
                            extractor.applyAsInt(opponent));
        }
    }

}
