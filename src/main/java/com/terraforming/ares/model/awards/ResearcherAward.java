package com.terraforming.ares.model.awards;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;

import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class ResearcherAward extends AbstractAward {

    @Override
    public ToIntFunction<Player> comparableParamExtractor(CardService cardService) {
        return player -> cardService.countPlayedTags(player, Set.of(Tag.SCIENCE));
    }

    @Override
    public AwardType getType() {
        return AwardType.RESEARCHER;
    }

    @Override
    public int getMaxValue() {
        return 18;//22
    }

}
