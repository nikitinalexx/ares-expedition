package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.awards.AbstractAward;
import com.terraforming.ares.model.awards.BaseAward;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.function.ToIntFunction;

public class AwardFeature implements FeatureBlock {
    private static final List<String> FEATURE_NAMES = List.of(
            "table_award_1_diff",
            "table_award_2_diff",
            "table_award_3_diff"
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
        Player player = ctx.getPlayer();
        Player opponent = ctx.getOpponent();

        CardService cardService = ctx.getCardService();


        List<BaseAward> awards = game.getAwards();
        for (BaseAward award : awards) {
            ToIntFunction<Player> awardProgressExtractor = ((AbstractAward) award).comparableParamExtractor(cardService);
            out.write((float) (Math.log1p(awardProgressExtractor.applyAsInt(player)) - Math.log1p(awardProgressExtractor.applyAsInt(opponent))));
        }
    }

}
