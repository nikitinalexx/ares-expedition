package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.dto.DraftCardsDto;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;

public class PlayerIncomesFeature implements FeatureBlock {
    public static final List<String> FEATURES = List.of(
            "win_points",
            "forests",

            "mc_income",
            "steel_income",
            "titanium_income",
            "plants_income",
            "heat_income",
            "card_income",
            "mc",
            "plants",
            "heat",
            "played_cards_count",
            "blue_cards_count",
            "green_cards_count",
            "hand_cards_count"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURES;
    }

    @Override
    public int size() {
        return 15;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        MarsGame game = ctx.getGame();
        Player player = ctx.getPlayer();
        WinPointsService winPointsService = ctx.getWinPointsService();
        CardService cardService = ctx.getCardService();

        out.write(winPointsService.countWinPointsWithoutResources(player, game) - player.getForests());
        out.write(player.getForests());

        out.write(player.getMcIncome() + player.getTerraformingRating());
        out.write(player.getSteelIncome());
        out.write(player.getTitaniumIncome());
        out.write(player.getPlantsIncome());
        out.write(player.getHeatIncome());
        out.write(player.getCardIncome());

        out.write(player.getMc());
        out.write(player.getPlants());
        out.write(player.getHeat());

        out.write(player.getPlayed().size() - 1);
        out.write(player.getPlayed().getCards().stream().map(cardService::getCard).filter(card -> card.getColor() == CardColor.BLUE).count());
        out.write(player.getPlayed().getCards().stream().map(cardService::getCard).filter(card -> card.getColor() == CardColor.GREEN).count());
        out.write(player.getHand().size());
    }

}
