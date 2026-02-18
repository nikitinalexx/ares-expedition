package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;

public class MarsAndPlayerIncomesFeature implements FeatureBlock {
    @Override
    public int size() {
        return 25;
    }

    public static final List<String> FEATURE_NAMES = List.of(
            "oxygen_left_ratio",
            "temperature_left_ratio",
            "oceans_left_ratio",

            "forests",
            "played_cards",
            "blue_cards_played",
            "green_cards_played",

            "mcincome_log",
            "plants",
            "plants_income",
            "heat_log",
            "heat_income_log",
            "card_income",
            "steel_income",
            "titanium_income",

            // Player phase cards (5 * 2)
            "player_phase0_first_upgrade",
            "player_phase0_second_upgrade",
            "player_phase1_first_upgrade",
            "player_phase1_second_upgrade",
            "player_phase2_first_upgrade",
            "player_phase2_second_upgrade",
            "player_phase3_first_upgrade",
            "player_phase3_second_upgrade",
            "player_phase4_first_upgrade",
            "player_phase4_second_upgrade"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }


    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Player player = ctx.getPlayer();
        CardService cardService = ctx.getCardService();
        MarsGame game = ctx.getGame();
        Planet planet = game.getPlanet();

        float oxygenLeftRatio = (float) planet.oxygenLeft() / planet.oxygenMax();
        float temperatureLeftRatio = (float) planet.temperatureLeft() / planet.temperatureMax();
        float oceansLeftRatio = (float) planet.oceansLeft() / planet.oceansMaxCount();

        out.write(oxygenLeftRatio);
        out.write(temperatureLeftRatio);
        out.write(oceansLeftRatio);

        out.write(player.getForests());

        out.write(player.getPlayed().size() - 1);
        out.write(player.getPlayed().getCards().stream().map(cardService::getCard).filter(card -> card.getColor() == CardColor.BLUE).count());
        out.write(player.getPlayed().getCards().stream().map(cardService::getCard).filter(card -> card.getColor() == CardColor.GREEN).count());

        out.write((float) Math.log1p(player.getMcIncome()));

        out.write((float) player.getPlants());
        out.write((float) player.getPlantsIncome());

        out.write((float) Math.log1p(player.getHeat()));
        out.write((float) Math.log1p(player.getHeatIncome()));

        out.write((float) player.getCardIncome());

        out.write(player.getSteelIncome());
        out.write(player.getTitaniumIncome());

        for (int i = 0; i < 5; i++) {
            out.write(player.getPhaseCards().get(i) == 1 ? 1 : 0);//first upgrade
            out.write(player.getPhaseCards().get(i) == 2 ? 1 : 0);//second upgrade
        }
    }
}
