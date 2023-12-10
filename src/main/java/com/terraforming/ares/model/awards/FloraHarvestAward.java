package com.terraforming.ares.model.awards;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

import java.util.function.ToIntFunction;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class FloraHarvestAward extends AbstractAward {

    @Override
    public ToIntFunction<Player> comparableParamExtractor(CardService cardService) {
        return Player::getPlantsIncome;
    }

    @Override
    public AwardType getType() {
        return AwardType.FLORA_HARVEST;
    }

    @Override
    public int getMaxValue() {
        return 18;//22
    }

}
