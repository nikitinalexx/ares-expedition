package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.dto.DraftCardsDto;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;

public class PlayerIncomesFeature implements FeatureBlock {
    public static final List<String> FEATURES = List.of(
            "win_points_without_tr",      // winPoints - TR
            "terraforming_rating",
            "mc_income",
            "steel_income",
            "titanium_income",
            "plants_income",
            "heat_income",
            "card_income",
            "mc",
            "plants",
            "heat",
            "heat_into_tr",
            "heat_to_tr",
            "played_cards_count",
            "hand_cards_count",
            "hand_over_10_count",
            "effective_count_max_10",
            "forests",
            "draft_cards_to_see",
            "draft_cards_to_take"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURES;
    }

    @Override
    public int size() {
        return 20;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        MarsGame game = ctx.getGame();
        Planet planet = game.getPlanet();
        Player player = ctx.getPlayer();
        WinPointsService winPointsService = ctx.getWinPointsService();
        DraftCardsService draftCardsService = ctx.getDraftCardsService();


        out.write(winPointsService.countWinPointsWithFloats(player, game) - player.getTerraformingRating());
        out.write(player.getTerraformingRating());
        out.write(player.getMcIncome());
        out.write(player.getSteelIncome());
        out.write(player.getTitaniumIncome());
        out.write(player.getPlantsIncome());
        out.write(player.getHeatIncome());
        out.write(player.getCardIncome());
        out.write(player.getMc());
        out.write(player.getPlants());
        out.write(player.getHeat());
        out.write(player.getHeat() / 8f);
        out.write((player.getHeat() % 8) / 8f);

        out.write(player.getPlayed().size());
        out.write(player.getHand().size());
        out.write(Math.max(0, player.getHand().size() - 10));
        out.write(Math.min(player.getHand().size(), 10));

        out.write(player.getForests());

        DraftCardsDto draftCardsDto = draftCardsService.countExtraCardsToTakeAndDraft(player);
        out.write(draftCardsDto.getCardsToSee());
        out.write(draftCardsDto.getCardsToTake());
    }

}
