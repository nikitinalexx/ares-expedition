package com.terraforming.ares.services.ai;

import com.terraforming.ares.model.Player;

/**
 * Created by oleksii.nikitin
 * Creation date 28.11.2022
 */
public class RandomBotHelper {
    public static final boolean FIRST_BOT_IS_RANDOM = true;

    public static boolean isRandomBot(Player player) {
        return false;
        //return true;
        // return player.isAi();
        //return player.isAi() && player.getUuid().endsWith("0") && FIRST_BOT_IS_RANDOM;
    }
}
