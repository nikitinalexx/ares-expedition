package com.terraforming.ares.model.awards;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import org.springframework.util.CollectionUtils;

import java.util.function.ToIntFunction;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class CriterionAward extends AbstractAward {

    @Override
    public AwardType getType() {
        return AwardType.CRITERION;
    }

    @Override
    public ToIntFunction<Player> comparableParamExtractor(CardService cardService) {
        return (player -> (int) player.getPlayed().getCards().stream().map(cardService::getCard)
                .filter(card -> !CollectionUtils.isEmpty(card.getTemperatureRequirement())
                        || !CollectionUtils.isEmpty(card.getOxygenRequirement())
                        || !CollectionUtils.isEmpty(card.getInfrastructureRequirement())
                        || card.getOceanRequirement() != null).count());
    }

    @Override
    public int getMaxValue() {
        return 15;
    }
}
