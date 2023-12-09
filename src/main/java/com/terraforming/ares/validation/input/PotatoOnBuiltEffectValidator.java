package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.Potatoes;
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
public class PotatoOnBuiltEffectValidator implements OnBuiltEffectValidator<Potatoes> {

    @Override
    public Class<Potatoes> getType() {
        return Potatoes.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        if (player.getPlants() < 2) {
            return "Not enough Plants to build the project";
        }

        return null;
    }
}
