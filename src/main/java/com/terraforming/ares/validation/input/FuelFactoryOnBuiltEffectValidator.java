package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.FuelFactory;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class FuelFactoryOnBuiltEffectValidator implements OnBuiltEffectValidator<FuelFactory> {

    @Override
    public Class<FuelFactory> getType() {
        return FuelFactory.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        if (player.getHeat() < 3) {
            return "Not enough Heat to build the project";
        }

        return null;
    }
}
