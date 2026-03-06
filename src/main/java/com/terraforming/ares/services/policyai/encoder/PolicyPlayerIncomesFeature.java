package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;

public class PolicyPlayerIncomesFeature implements PlayerPolicyFeatureBlock {

    @Override
    public void encode(TableContext ctx, PolicyRecord out, int playerIndex) {
        MarsGame game = ctx.getGame();
        Player player = ctx.getPlayer();
        WinPointsService winPointsService = ctx.getWinPointsService();
        CardService cardService = ctx.getCardService();

        out.winPointsNoForests[playerIndex] =
                (short) (winPointsService
                        .countWinPointsWithoutResources(player, game)
                        - player.getForests());

        out.forests[playerIndex] =
                (short) player.getForests();

        out.mcIncomeTotal[playerIndex] =
                (short) (player.getMcIncome()
                        + player.getTerraformingRating());

        out.steelIncome[playerIndex] = (byte) player.getSteelIncome();
        out.titaniumIncome[playerIndex] = (byte) player.getTitaniumIncome();
        out.plantsIncome[playerIndex] = (byte) player.getPlantsIncome();
        out.heatIncome[playerIndex] = (byte) player.getHeatIncome();
        out.cardIncome[playerIndex] = (byte) player.getCardIncome();

        out.mc[playerIndex] = (short) player.getMc();
        out.plants[playerIndex] = (short) player.getPlants();
        out.heat[playerIndex] = (short) player.getHeat();

        int blue = 0;
        int green = 0;

        for (int cardId : player.getPlayed().getCards()) {
            Card card = cardService.getCard(cardId);

            if (card.getColor() == CardColor.BLUE)
                blue++;
            else if (card.getColor() == CardColor.GREEN)
                green++;
        }

        out.playedCards[playerIndex] = (short) (Math.max(player.getPlayed().size() - 1, 0));
        out.blueCards[playerIndex] = (short) blue;
        out.greenCards[playerIndex] = (short) green;
        out.handSize[playerIndex] = (byte) player.getHand().size();
    }

}
