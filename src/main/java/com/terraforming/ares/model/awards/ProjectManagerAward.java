package com.terraforming.ares.model.awards;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

import java.util.function.ToIntFunction;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class ProjectManagerAward extends AbstractAward {
    public static final ToIntFunction<Player> valueExtractor = (player -> player.getPlayed().size());


    @Override
    public AwardType getType() {
        return AwardType.PROJECT_MANAGER;
    }

    @Override
    public ToIntFunction<Player> comparableParamExtractor(CardService cardService) {
        return valueExtractor;
    }

    @Override
    public int getMaxValue() {
        return 49;//61
    }
}
