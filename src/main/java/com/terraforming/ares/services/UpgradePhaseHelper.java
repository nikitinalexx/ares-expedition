package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 18.02.2023
 */
public class UpgradePhaseHelper {

    public static void upgradePhase(CardService cardService, MarsGame game, Player player, int upgrade) {
        //0 to 4. represents 1 to 5 phase.
        int phaseIndex = upgrade / 2;

        //0 is default phase. 1 is first upgrade, 2 is second upgrade.
        int upgradeType = upgrade % 2 + 1;

        //todo remove after testing
//        if (!player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_CARD)
//                && phaseIndex == 1 && upgradeType == 2 //second phase and second upgrade
//                && !player.isGotBonusInSecondPhase()
//                && player.getActionsInSecondPhase() >= 1
//                && player.getChosenPhase() == 2) {
//            player.getHand().addCards(cardService.dealCards(game, 1));
//        }
//
//        if (player.hasPhaseUpgrade(Constants.PHASE_3_NO_UPGRADE)
//                && game.getCurrentPhase() == 3
//                && player.getChosenPhase() == 3) {
//            if (phaseIndex == 2 && upgradeType == 1) { //third phase and first upgrade)
//                player.setBlueActionExtraActivationsLeft(player.getBlueActionExtraActivationsLeft() + 1);
//            } else if (phaseIndex == 2 && upgradeType == 2) { //third phase and second upgrade)
//                final List<Integer> cards = cardService.dealCards(game, 3);
//                for (Integer cardId : cards) {
//                    final Card card = cardService.getCard(cardId);
//                    if (card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED) {
//                        player.getHand().addCard(cardId);
//                    }
//                }
//            }
//        }

        player.updatePhaseCard(phaseIndex, upgradeType);
    }
}
