package com.terraforming.ares.model.awards;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

import java.util.function.ToIntFunction;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class CelebrityAward extends AbstractAward {
    public static final ToIntFunction<Player> valueExtractor = Player::getMcIncome;

    @Override
    public AwardType getType() {
        return AwardType.CELEBRITY;
    }

    @Override
    public ToIntFunction<Player> comparableParamExtractor(CardService cardService) {
        return valueExtractor;
    }

    @Override
    public int getMaxValue() {
        return 96;//121
    }

}
