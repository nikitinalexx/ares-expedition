package com.terraforming.ares.model.awards;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

import java.util.function.ToIntFunction;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class GeneratorAward extends AbstractAward {
    public static final ToIntFunction<Player> valueExtractor = Player::getHeatIncome;


    @Override
    public ToIntFunction<Player> comparableParamExtractor(CardService cardService) {
        return valueExtractor;
    }

    @Override
    public AwardType getType() {
        return AwardType.GENERATOR;
    }

    @Override
    public int getMaxValue() {
        return 56;//50
    }
}
